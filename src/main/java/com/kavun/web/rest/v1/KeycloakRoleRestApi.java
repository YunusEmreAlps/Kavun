package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.service.user.RoleService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.constant.AdminConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keycloak-enabled Role Management REST API controller.
 * Manages roles in both Keycloak and local database.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "03. Role Management", description = "Keycloak-integrated role management APIs")
@RequestMapping(AdminConstants.API_V1_ROLE_ROOT_URL)
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakRoleRestApi {

  private final RoleService roleService;
  private final Keycloak keycloakAdmin;
  private final KeycloakProperties keycloakProperties;

  private static final String AUTHORIZE_ADMIN = "hasRole('ROLE_ADMIN')";

  /**
   * Gets all roles from local database.
   *
   * @return list of roles
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get All Local Roles", description = "Retrieve all roles from local database")
  public ResponseEntity<List<Role>> getAllRoles() {
    List<Role> roles = roleService.findAll();
    return ResponseEntity.ok(roles);
  }

  /**
   * Gets all roles from Keycloak.
   *
   * @return list of Keycloak roles
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/keycloak", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get All Keycloak Roles", description = "Retrieve all roles from Keycloak")
  public ResponseEntity<?> getAllKeycloakRoles() {
    try {
      List<RoleRepresentation> roles = keycloakAdmin
          .realm(keycloakProperties.getRealm())
          .roles()
          .list();

      var roleList = roles.stream()
          .map(role -> Map.of(
              "name", role.getName(),
              "description", role.getDescription() != null ? role.getDescription() : "",
              "composite", role.isComposite()
          ))
          .collect(Collectors.toList());

      return ResponseEntity.ok(roleList);
    } catch (Exception e) {
      LOG.error("Error fetching Keycloak roles", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to fetch roles from Keycloak"));
    }
  }

  /**
   * Gets a role by ID from local database.
   *
   * @param id the role ID
   * @return the role
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get Role by ID", description = "Retrieve role by ID from local database")
  public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
    Role role = roleService.findById(id);
    if (role == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(role);
  }

  /**
   * Gets a role by public ID from local database.
   *
   * @param publicId the public ID
   * @return the role
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/public/{publicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get Role by Public ID", description = "Retrieve role by public ID")
  public ResponseEntity<Role> getRoleByPublicId(@PathVariable String publicId) {
    Role role = roleService.findByPublicId(publicId);
    if (role == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(role);
  }

  /**
   * Gets a role by name from local database.
   *
   * @param name the role name
   * @return the role
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get Role by Name", description = "Retrieve role by name")
  public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
    Role role = roleService.findByName(name);
    if (role == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(role);
  }

  /**
   * Gets all roles with pagination from local database.
   *
   * @param pageable pagination information
   * @return paginated list of roles
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/pageable", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get All Roles Pageable", description = "Retrieve all roles with pagination")
  public ResponseEntity<Page<Role>> getAllRolesPageable(Pageable pageable) {
    Page<Role> roles = roleService.findAll(pageable);
    return ResponseEntity.ok(roles);
  }

  /**
   * Creates a new role in Keycloak.
   *
   * @param roleRequest the role creation request
   * @return success message
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @PostMapping(value = "/keycloak", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create Keycloak Role", description = "Create a new role in Keycloak")
  public ResponseEntity<?> createKeycloakRole(@RequestBody Map<String, String> roleRequest) {
    try {
      String roleName = roleRequest.get("name");
      String description = roleRequest.get("description");

      if (roleName == null || roleName.isBlank()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Role name is required"));
      }

      RoleRepresentation role = new RoleRepresentation();
      role.setName(roleName.toLowerCase());
      role.setDescription(description);

      keycloakAdmin
          .realm(keycloakProperties.getRealm())
          .roles()
          .create(role);

      LOG.info("Created role in Keycloak: {}", roleName);

      return ResponseEntity.ok(Map.of(
          "message", "Role created successfully",
          "name", roleName
      ));
    } catch (Exception e) {
      LOG.error("Error creating Keycloak role", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to create role in Keycloak: " + e.getMessage()));
    }
  }
}
