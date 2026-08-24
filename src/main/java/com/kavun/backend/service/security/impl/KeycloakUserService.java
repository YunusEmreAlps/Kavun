package com.kavun.backend.service.security.impl;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.domain.user.UserHistory;
import com.kavun.backend.persistent.repository.UserRepository;
import com.kavun.backend.service.user.RoleService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.config.security.keycloak.KeycloakRoleClaims;
import com.kavun.enums.RoleType;
import com.kavun.enums.UserHistoryType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
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
 * Service for just-in-time provisioning/synchronization of Keycloak-authenticated users into the
 * local database, plus Keycloak admin-client operations for managing accounts directly in
 * Keycloak.
 *
 * <p>JIT sync keeps a local {@code User} row (with the matching {@code ROLE_ADMIN}/{@code
 * ROLE_USER} local {@code Role}) in step with the Keycloak identity, so the existing
 * page/action-based {@code Permission} system keeps working unmodified regardless of which
 * identity provider authenticated the request.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
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
   * Synchronizes a user from a Bearer JWT (API path) to the local database.
   *
   * @param jwt the Keycloak JWT token
   * @return the synchronized user
   */
  @Transactional
  public User syncUserFromKeycloak(Jwt jwt) {
    return syncUserFromClaims(jwt.getClaims());
  }

  /**
   * Synchronizes a user from a generic claims map (ID token/userinfo claims for the web-login
   * path, or JWT claims for the API path) to the local database. Creates the user if not present,
   * otherwise only persists a change if something actually changed.
   *
   * @param claims the token/userinfo claims
   * @return the synchronized user
   */
  @Transactional
  public User syncUserFromClaims(Map<String, Object> claims) {
    String keycloakId = (String) claims.get("sub");
    String username = (String) claims.get("preferred_username");
    String email = (String) claims.get("email");
    String firstName = (String) claims.get("given_name");
    String lastName = (String) claims.get("family_name");

    LOG.debug("Syncing user from Keycloak claims: keycloakId={}, username={}", keycloakId, username);

    User user = userRepository.findByUsername(username);
    boolean isNew = user == null;

    if (isNew) {
      user = createLocalUser(keycloakId, username, email, firstName, lastName);
    }

    boolean fieldsChanged = !isNew && updateLocalUser(user, email, firstName, lastName);
    boolean rolesChanged = syncRolesFromClaims(user, claims);

    if (isNew || fieldsChanged || rolesChanged) {
      user = userRepository.save(user);
      LOG.info("{} local user from Keycloak: {}", isNew ? "Created" : "Updated", username);
    }

    return user;
  }

  /**
   * Synchronizes a user from claims and returns it as a {@link UserDto}, for use by the
   * permission bridge that resolves the acting user for {@code @RequirePermission} checks.
   *
   * @param claims the token/userinfo claims
   * @return the synchronized user as a DTO
   */
  @Transactional
  public UserDto resolveOrProvisionUserDto(Map<String, Object> claims) {
    return UserUtils.convertToUserDto(syncUserFromClaims(claims));
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

  /**
   * Updates a local user's profile fields from Keycloak claims.
   *
   * @return true if any field actually changed
   */
  private boolean updateLocalUser(User user, String email, String firstName, String lastName) {
    boolean changed = false;
    if (email != null && !email.equals(user.getEmail())) {
      user.setEmail(email);
      changed = true;
    }
    if (firstName != null && !firstName.equals(user.getFirstName())) {
      user.setFirstName(firstName);
      changed = true;
    }
    if (lastName != null && !lastName.equals(user.getLastName())) {
      user.setLastName(lastName);
      changed = true;
    }
    return changed;
  }

  /**
   * Syncs realm/client roles from claims onto the local user.
   *
   * @return true if any role was newly added
   */
  private boolean syncRolesFromClaims(User user, Map<String, Object> claims) {
    boolean changed = false;
    for (String roleName : KeycloakRoleClaims.extractRoleNames(claims, keycloakProperties.getClientId())) {
      changed |= addRoleToUser(user, roleName);
    }
    return changed;
  }

  /**
   * @return true if the role was a known application role and was newly added to the user
   */
  private boolean addRoleToUser(User user, String roleName) {
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
          return true;
        }
      }
    } catch (IllegalArgumentException e) {
      // Role doesn't exist in our enum, skip it
      LOG.debug("Skipping unknown role from Keycloak: {}", roleName);
    }
    return false;
  }

  private String extractUserId(Response response) {
    String location = response.getHeaderString("Location");
    if (location != null) {
      return location.substring(location.lastIndexOf('/') + 1);
    }
    return null;
  }
}
