package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.specification.UserSpecification;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.impl.KeycloakUserService;
import com.kavun.backend.service.user.UserService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.constant.AdminConstants;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.shared.util.UserUtils;
import com.kavun.web.payload.request.SignUpRequest;
import com.kavun.web.payload.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Keycloak-enabled User Management REST API controller.
 * Handles user operations with Keycloak integration.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminConstants.API_V1_USERS_ROOT_URL)
@Tag(name = "02. User Management", description = "Keycloak-integrated user management APIs")
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakUserRestApi {

  private final UserService userService;
  private final EmailService emailService;
  private final KeycloakUserService keycloakUserService;
  private final KeycloakProperties keycloakProperties;
  private final UserSpecification userSpecification;

  private static final String AUTHORIZE_ADMIN = "hasRole('ROLE_ADMIN')";
  private static final String AUTHORIZE_AUTHENTICATED = "isAuthenticated()";

  /**
   * Searches for users based on the provided parameters.
   *
   * @param parameterMap a map of search parameters
   * @param page         pagination information
   * @return a paginated list of users that match the search criteria
   */
  @Loggable
  @PageableAsQueryParam
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Search Users", description = "Search users with filters")
  public ResponseEntity<Page<UserResponse>> searchUsers(
      @RequestParam Map<String, Object> parameterMap,
      Pageable page) {

    Specification<User> spec = userSpecification.search(parameterMap);
    Page<UserResponse> users = userService.findAll(spec, page);
    return ResponseEntity.ok(users);
  }

  /**
   * Retrieves all users with pagination.
   *
   * @param page pagination information
   * @return paginated list of users
   */
  @Loggable
  @PageableAsQueryParam
  @PreAuthorize(AUTHORIZE_ADMIN)
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get All Users", description = "Retrieve all users with pagination")
  public ResponseEntity<Page<UserResponse>> getUsers(final Pageable page) {
    Page<UserResponse> users = userService.findAll(page);
    return ResponseEntity.ok(users);
  }

  /**
   * Creates a new user in both Keycloak and local database.
   *
   * @param signUpRequest the user registration details
   * @return response with location header of the created user
   */
  @Loggable
  @PostMapping
  @SecurityRequirements
  @Operation(summary = "Register User", description = "Create new user in Keycloak and local database")
  public ResponseEntity<?> createUser(@Valid @RequestBody SignUpRequest signUpRequest) {
    LOG.info("User registration request for: {}", signUpRequest.getUsername());

    // Check if user exists in local DB
    if (userService.existsByUsernameOrEmailAndEnabled(
        signUpRequest.getUsername(), signUpRequest.getEmail())) {
      LOG.warn("Username or email already exists: {}", signUpRequest.getUsername());
      return ResponseEntity.badRequest()
          .body(Map.of("error", UserConstants.USERNAME_OR_EMAIL_EXISTS));
    }

    // Check if user exists in Keycloak
    Optional<org.keycloak.representations.idm.UserRepresentation> existingKeycloakUser =
        keycloakUserService.findKeycloakUserByUsername(signUpRequest.getUsername());
    if (existingKeycloakUser.isPresent()) {
      LOG.warn("User already exists in Keycloak: {}", signUpRequest.getUsername());
      return ResponseEntity.badRequest()
          .body(Map.of("error", "User already exists in authentication system"));
    }

    // Create user in Keycloak first
    Optional<String> keycloakUserId = keycloakUserService.createKeycloakUser(
        signUpRequest.getUsername(),
        signUpRequest.getEmail(),
        signUpRequest.getPassword(),
        signUpRequest.getFirstName(),
        signUpRequest.getLastName(),
        Set.of("user") // Default role
    );

    if (keycloakUserId.isEmpty()) {
      LOG.error("Failed to create user in Keycloak: {}", signUpRequest.getUsername());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to create user in authentication system"));
    }

    // Create user in local database
    var userDto = UserUtils.convertToUserDto(signUpRequest);
    userDto.setPublicId(keycloakUserId.get()); // Use Keycloak ID as public ID
    userDto.setPassword("KEYCLOAK_MANAGED"); // Password managed by Keycloak

    var savedUserDto = userService.createUser(userDto);

    // Send welcome email
    emailService.sendAccountVerificationEmail(savedUserDto, null);

    var location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{publicId}")
        .buildAndExpand(savedUserDto.getPublicId())
        .toUriString();

    LOG.info("User created successfully: {} with Keycloak ID: {}",
        signUpRequest.getUsername(), keycloakUserId.get());

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION, location)
        .body(Map.of(
            "message", "User created successfully",
            "publicId", savedUserDto.getPublicId(),
            "username", savedUserDto.getUsername()
        ));
  }

  /**
   * Deletes a user from both Keycloak and local database.
   *
   * @param publicId the user's public ID (Keycloak user ID)
   * @return operation status
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @DeleteMapping(value = "/{publicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Delete User", description = "Delete user from Keycloak and local database")
  public ResponseEntity<OperationStatus> deleteUser(@PathVariable String publicId) {
    LOG.info("Deleting user: {}", publicId);

    // Delete from Keycloak first
    keycloakUserService.deleteKeycloakUser(publicId);

    // Delete from local database
    userService.deleteUser(publicId);

    return ResponseEntity.ok(OperationStatus.SUCCESS);
  }

  /**
   * Enables a user in both Keycloak and local database.
   *
   * @param publicId the user's public ID
   * @return operation status
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @PutMapping(value = "/{publicId}/enable")
  @Operation(summary = "Enable User", description = "Enable user in Keycloak and local database")
  public ResponseEntity<OperationStatus> enableUser(@PathVariable String publicId) {
    LOG.info("Enabling user: {}", publicId);

    // Enable in Keycloak
    keycloakUserService.setUserEnabled(publicId, true);

    // Enable in local database
    var userDto = userService.enableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Disables a user in both Keycloak and local database.
   *
   * @param publicId the user's public ID
   * @return operation status
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @PutMapping(value = "/{publicId}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Disable User", description = "Disable user in Keycloak and local database")
  public ResponseEntity<OperationStatus> disableUser(@PathVariable String publicId) {
    LOG.info("Disabling user: {}", publicId);

    // Disable in Keycloak
    keycloakUserService.setUserEnabled(publicId, false);

    // Disable in local database
    var userDto = userService.disableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Updates password for the currently authenticated user.
   *
   * @param jwt         the JWT token of the authenticated user
   * @param newPassword the new password
   * @return success message
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_AUTHENTICATED)
  @PostMapping(value = UserConstants.UPDATE_PASSWORD_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Update Password", description = "Update password for authenticated user in Keycloak")
  public ResponseEntity<?> updatePassword(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String newPassword) {

    if (jwt == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", ErrorConstants.UNAUTHORIZED_ACCESS));
    }

    String keycloakUserId = jwt.getSubject();
    LOG.info("Updating password for user: {}", keycloakUserId);

    // Update password in Keycloak
    keycloakUserService.updatePassword(keycloakUserId, newPassword);

    return ResponseEntity.ok(Map.of("message", UserConstants.PASSWORD_UPDATED_SUCCESSFULLY));
  }

  /**
   * Assigns roles to a user in Keycloak.
   *
   * @param publicId the user's public ID (Keycloak user ID)
   * @param roles    the roles to assign
   * @return operation status
   */
  @Loggable
  @PreAuthorize(AUTHORIZE_ADMIN)
  @PutMapping(value = "/{publicId}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Assign Roles", description = "Assign roles to user in Keycloak")
  public ResponseEntity<?> assignRoles(
      @PathVariable String publicId,
      @RequestBody Set<String> roles) {

    LOG.info("Assigning roles {} to user: {}", roles, publicId);

    keycloakUserService.assignRolesToUser(publicId, roles);

    return ResponseEntity.ok(Map.of(
        "message", "Roles assigned successfully",
        "roles", roles
    ));
  }
}
