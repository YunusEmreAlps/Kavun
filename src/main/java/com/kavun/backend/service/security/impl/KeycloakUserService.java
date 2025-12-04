package com.kavun.backend.service.security.impl;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.user.RoleService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.enums.RoleType;
import com.kavun.enums.UserHistoryType;
import com.kavun.backend.persistent.domain.user.UserHistory;
import com.nimbusds.jose.JWSObject;
import jakarta.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing users in Keycloak and synchronizing with local database.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakUserService {

  private final Keycloak keycloakAdmin;
  private final KeycloakProperties keycloakProperties;
  private final UserRepository userRepository;
  private final RoleService roleService;

  /**
   * Synchronizes user from Keycloak JWT to local database.
   * Creates user if not exists, updates if exists.
   *
   * @param jwt the Keycloak JWT token
   * @return the synchronized user
   */
  @Transactional
  public User syncUserFromKeycloak(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    String username = jwt.getClaimAsString("preferred_username");
    String email = jwt.getClaimAsString("email");
    String firstName = jwt.getClaimAsString("given_name");
    String lastName = jwt.getClaimAsString("family_name");

    LOG.debug("Syncing user from Keycloak: keycloakId={}, username={}", keycloakId, username);

    // Try to find existing user by keycloak ID or username
    User user = userRepository.findByUsername(username);

    if (user == null) {
      // Create new user in local database
      user = createLocalUser(keycloakId, username, email, firstName, lastName);
      LOG.info("Created new local user from Keycloak: {}", username);
    } else {
      // Update existing user
      updateLocalUser(user, email, firstName, lastName);
      LOG.debug("Updated local user from Keycloak: {}", username);
    }

    // Sync roles from JWT
    syncRolesFromJwt(user, jwt);

    return userRepository.save(user);
  }

  /**
   * Synchronizes user from raw JWT access token string to local database.
   * Creates user if not exists, updates if exists.
   *
   * @param accessToken the raw JWT access token string
   * @return the synchronized user
   */
  @Transactional
  @SuppressWarnings("unchecked")
  public User syncUserFromToken(String accessToken) {
    try {
      // Parse the JWT token
      JWSObject jwsObject = JWSObject.parse(accessToken);
      Map<String, Object> claims = jwsObject.getPayload().toJSONObject();

      String keycloakId = (String) claims.get("sub");
      String username = (String) claims.get("preferred_username");
      String email = (String) claims.get("email");
      String firstName = (String) claims.get("given_name");
      String lastName = (String) claims.get("family_name");

      LOG.debug("Syncing user from token: keycloakId={}, username={}", keycloakId, username);

      // Try to find existing user by username
      User user = userRepository.findByUsername(username);

      if (user == null) {
        // Create new user in local database
        user = createLocalUser(keycloakId, username, email, firstName, lastName);
        LOG.info("Created new local user from Keycloak token: {}", username);
      } else {
        // Update existing user
        updateLocalUser(user, email, firstName, lastName);
        LOG.debug("Updated local user from Keycloak token: {}", username);
      }

      // Sync roles from token claims
      syncRolesFromClaims(user, claims);

      return userRepository.save(user);
    } catch (ParseException e) {
      LOG.error("Failed to parse JWT token: {}", e.getMessage());
      throw new RuntimeException("Failed to parse JWT token", e);
    }
  }

  /**
   * Extracts roles from a raw JWT access token string.
   *
   * @param accessToken the raw JWT access token string
   * @return set of role names
   */
  @SuppressWarnings("unchecked")
  public Set<String> extractRolesFromToken(String accessToken) {
    Set<String> roles = new HashSet<>();
    try {
      JWSObject jwsObject = JWSObject.parse(accessToken);
      Map<String, Object> claims = jwsObject.getPayload().toJSONObject();

      // Extract realm roles
      Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
      if (realmAccess != null && realmAccess.get("roles") instanceof List) {
        List<String> realmRoles = (List<String>) realmAccess.get("roles");
        roles.addAll(realmRoles);
      }

      // Extract client roles
      Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
      if (resourceAccess != null) {
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(keycloakProperties.getClientId());
        if (clientAccess != null && clientAccess.get("roles") instanceof List) {
          List<String> clientRoles = (List<String>) clientAccess.get("roles");
          roles.addAll(clientRoles);
        }
      }

      LOG.debug("Extracted roles from token: {}", roles);
    } catch (ParseException e) {
      LOG.error("Failed to parse JWT token for role extraction: {}", e.getMessage());
    }
    return roles;
  }

  /**
   * Creates a new user in Keycloak.
   *
   * @param username  the username
   * @param email     the email
   * @param password  the password
   * @param firstName the first name
   * @param lastName  the last name
   * @param roles     the roles to assign
   * @return the Keycloak user ID if successful, empty otherwise
   */
  public Optional<String> createKeycloakUser(String username, String email, String password,
                                              String firstName, String lastName, Set<String> roles) {
    try {
      UsersResource usersResource = getRealmResource().users();

      // Check if user already exists
      List<UserRepresentation> existingUsers = usersResource.search(username, true);
      if (!existingUsers.isEmpty()) {
        LOG.warn("User already exists in Keycloak: {}", username);
        return Optional.empty();
      }

      // Create user representation
      UserRepresentation userRep = new UserRepresentation();
      userRep.setEnabled(true);
      userRep.setUsername(username);
      userRep.setEmail(email);
      userRep.setFirstName(firstName);
      userRep.setLastName(lastName);
      userRep.setEmailVerified(false);

      // Set password credential
      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(password);
      credential.setTemporary(false);
      userRep.setCredentials(Collections.singletonList(credential));

      // Create user
      try (Response response = usersResource.create(userRep)) {
        if (response.getStatus() == 201) {
          String userId = extractUserId(response);
          LOG.info("Created Keycloak user: {} with ID: {}", username, userId);

          // Assign roles
          if (roles != null && !roles.isEmpty()) {
            assignRolesToUser(userId, roles);
          }

          return Optional.ofNullable(userId);
        } else {
          LOG.error("Failed to create Keycloak user: {} - Status: {}", username, response.getStatus());
          return Optional.empty();
        }
      }
    } catch (Exception e) {
      LOG.error("Error creating Keycloak user: {}", username, e);
      return Optional.empty();
    }
  }

  /**
   * Updates a user in Keycloak.
   *
   * @param keycloakUserId the Keycloak user ID
   * @param email          the email
   * @param firstName      the first name
   * @param lastName       the last name
   */
  public void updateKeycloakUser(String keycloakUserId, String email, String firstName, String lastName) {
    try {
      UserResource userResource = getRealmResource().users().get(keycloakUserId);
      UserRepresentation userRep = userResource.toRepresentation();

      userRep.setEmail(email);
      userRep.setFirstName(firstName);
      userRep.setLastName(lastName);

      userResource.update(userRep);
      LOG.info("Updated Keycloak user: {}", keycloakUserId);
    } catch (Exception e) {
      LOG.error("Error updating Keycloak user: {}", keycloakUserId, e);
    }
  }

  /**
   * Updates user password in Keycloak.
   *
   * @param keycloakUserId the Keycloak user ID
   * @param newPassword    the new password
   */
  public void updatePassword(String keycloakUserId, String newPassword) {
    try {
      UserResource userResource = getRealmResource().users().get(keycloakUserId);

      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(newPassword);
      credential.setTemporary(false);

      userResource.resetPassword(credential);
      LOG.info("Updated password for Keycloak user: {}", keycloakUserId);
    } catch (Exception e) {
      LOG.error("Error updating password for Keycloak user: {}", keycloakUserId, e);
    }
  }

  /**
   * Deletes a user from Keycloak.
   *
   * @param keycloakUserId the Keycloak user ID
   */
  public void deleteKeycloakUser(String keycloakUserId) {
    try {
      getRealmResource().users().delete(keycloakUserId);
      LOG.info("Deleted Keycloak user: {}", keycloakUserId);
    } catch (Exception e) {
      LOG.error("Error deleting Keycloak user: {}", keycloakUserId, e);
    }
  }

  /**
   * Enables or disables a user in Keycloak.
   *
   * @param keycloakUserId the Keycloak user ID
   * @param enabled        true to enable, false to disable
   */
  public void setUserEnabled(String keycloakUserId, boolean enabled) {
    try {
      UserResource userResource = getRealmResource().users().get(keycloakUserId);
      UserRepresentation userRep = userResource.toRepresentation();
      userRep.setEnabled(enabled);
      userResource.update(userRep);
      LOG.info("{} Keycloak user: {}", enabled ? "Enabled" : "Disabled", keycloakUserId);
    } catch (Exception e) {
      LOG.error("Error {} Keycloak user: {}", enabled ? "enabling" : "disabling", keycloakUserId, e);
    }
  }

  /**
   * Assigns roles to a user in Keycloak.
   *
   * @param keycloakUserId the Keycloak user ID
   * @param roleNames      the role names to assign
   */
  public void assignRolesToUser(String keycloakUserId, Set<String> roleNames) {
    try {
      UserResource userResource = getRealmResource().users().get(keycloakUserId);
      RolesResource rolesResource = getRealmResource().roles();

      List<RoleRepresentation> rolesToAdd = new ArrayList<>();
      for (String roleName : roleNames) {
        try {
          // Try to get the role, remove ROLE_ prefix if present
          String keycloakRoleName = roleName.startsWith("ROLE_")
              ? roleName.substring(5).toLowerCase()
              : roleName.toLowerCase();

          RoleRepresentation role = rolesResource.get(keycloakRoleName).toRepresentation();
          rolesToAdd.add(role);
        } catch (Exception e) {
          LOG.warn("Role not found in Keycloak: {}", roleName);
        }
      }

      if (!rolesToAdd.isEmpty()) {
        userResource.roles().realmLevel().add(rolesToAdd);
        LOG.info("Assigned roles {} to Keycloak user: {}", roleNames, keycloakUserId);
      }
    } catch (Exception e) {
      LOG.error("Error assigning roles to Keycloak user: {}", keycloakUserId, e);
    }
  }

  /**
   * Finds a Keycloak user by username.
   *
   * @param username the username
   * @return the user representation if found
   */
  public Optional<UserRepresentation> findKeycloakUserByUsername(String username) {
    try {
      List<UserRepresentation> users = getRealmResource().users().search(username, true);
      return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    } catch (Exception e) {
      LOG.error("Error finding Keycloak user by username: {}", username, e);
      return Optional.empty();
    }
  }

  /**
   * Finds a Keycloak user by email.
   *
   * @param email the email
   * @return the user representation if found
   */
  public Optional<UserRepresentation> findKeycloakUserByEmail(String email) {
    try {
      List<UserRepresentation> users = getRealmResource().users().searchByEmail(email, true);
      return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    } catch (Exception e) {
      LOG.error("Error finding Keycloak user by email: {}", email, e);
      return Optional.empty();
    }
  }

  // =========================================================================
  // PRIVATE HELPER METHODS
  // =========================================================================

  private RealmResource getRealmResource() {
    return keycloakAdmin.realm(keycloakProperties.getRealm());
  }

  private User createLocalUser(String keycloakId, String username, String email,
                               String firstName, String lastName) {
    User user = new User();
    user.setPublicId(keycloakId);
    user.setUsername(username);
    user.setEmail(email != null ? email : username + "@keycloak.local");
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setPassword("KEYCLOAK_MANAGED"); // Password is managed by Keycloak
    user.setEnabled(true);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(true);
    user.setCredentialsNonExpired(true);

    // Add default role
    var defaultRole = roleService.findByName(RoleType.ROLE_USER.name());
    if (defaultRole != null) {
      user.addUserRole(defaultRole);
    }

    // Add user history
    user.addUserHistory(new UserHistory(UUID.randomUUID().toString(), user, UserHistoryType.CREATED));

    return user;
  }

  private void updateLocalUser(User user, String email, String firstName, String lastName) {
    if (email != null) {
      user.setEmail(email);
    }
    if (firstName != null) {
      user.setFirstName(firstName);
    }
    if (lastName != null) {
      user.setLastName(lastName);
    }
  }

  @SuppressWarnings("unchecked")
  private void syncRolesFromJwt(User user, Jwt jwt) {
    // Extract roles from JWT
    var realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof List) {
      List<String> jwtRoles = (List<String>) realmAccess.get("roles");

      for (String roleName : jwtRoles) {
        addRoleToUser(user, roleName);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void syncRolesFromClaims(User user, Map<String, Object> claims) {
    // Extract realm roles
    Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
    if (realmAccess != null && realmAccess.get("roles") instanceof List) {
      List<String> jwtRoles = (List<String>) realmAccess.get("roles");

      for (String roleName : jwtRoles) {
        addRoleToUser(user, roleName);
      }
    }
  }

  private void addRoleToUser(User user, String roleName) {
    String normalizedRoleName = roleName.toUpperCase();
    if (!normalizedRoleName.startsWith("ROLE_")) {
      normalizedRoleName = "ROLE_" + normalizedRoleName;
    }

    // Check if this is a valid application role
    try {
      RoleType roleType = RoleType.valueOf(normalizedRoleName);
      var role = roleService.findByName(roleType.name());
      if (role != null) {
        boolean hasRole = user.getUserRoles().stream()
            .anyMatch(ur -> ur.getRole().getName().equals(role.getName()));
        if (!hasRole) {
          user.addUserRole(role);
          LOG.debug("Added role {} to user {}", role.getName(), user.getUsername());
        }
      }
    } catch (IllegalArgumentException e) {
      // Role doesn't exist in our enum, skip it
      LOG.debug("Skipping unknown role from Keycloak: {}", roleName);
    }
  }

  private String extractUserId(Response response) {
    String location = response.getHeaderString("Location");
    if (location != null) {
      return location.substring(location.lastIndexOf('/') + 1);
    }
    return null;
  }
}
