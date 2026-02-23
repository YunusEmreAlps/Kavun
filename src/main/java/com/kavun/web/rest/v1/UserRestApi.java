package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.persistent.specification.UserSpecification;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.AdminConstants;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.mapper.UserMapper;
import com.kavun.shared.request.UserRequest;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class handles all rest calls for users.
 *
 * <p><b>Note:</b> This controller is only active when {@code keycloak.enabled=false}.
 * When Keycloak is enabled, use {@link KeycloakUserRestApi} instead.</p>
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 * @see KeycloakUserRestApi
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminConstants.API_V1_USERS_ROOT_URL)
@Tag(name = "02. User Management", description = "User management APIs (Local mode)")
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false", matchIfMissing = true)
public class UserRestApi {

  private final UserService userService;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final EncryptionService encryptionService;
  private final UserSpecification userSpecification;
  private final UserMapper userMapper;

  /**
   * Searches for users based on the provided parameters
   *
   * @param paramaterMap a map of search parameters where the key is the
   * @param page         pagination information
   * @return a paginated list of users that match the search criteria
   */
  @Loggable
  @PageableAsQueryParam
  @GetMapping(value = "/paging", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<UserResponse>> searchUsers(
      @RequestParam Map<String, Object> paramaterMap,
      Pageable page) {

    Specification<User> spec = userSpecification.search(paramaterMap);

    Page<UserDto> userDtos = userService.findAll(spec, page);
    Page<UserResponse> users = userDtos.map(userDto -> userMapper.toUserResponse(UserUtils.convertToUser(userDto)));
    return ResponseEntity.ok(users);
  }

  /**
   * Retrieves a single user by id.
   *
   * @param id the user Long
   * @return the user details
   */
  @Loggable
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
    UserDto userDto = userService.findById(id);
    if (userDto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(userMapper.toUserResponse(UserUtils.convertToUser(userDto)));
  }

  /**
   * Soft deletes the user associated with the id.
   *
   * @param id the user Long
   * @return if the operation is success
   */
  @Loggable
  @RequirePermission(autoDetect = true)
  @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> softDeleteUser(@PathVariable Long id) {
    boolean result = userService.softDeleteUser(id);

    if (!result) {
      LOG.warn("Failed to soft delete user with id: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(OperationStatus.FAILURE);
    }

    LOG.info("User with id {} soft deleted successfully", id);
    return ResponseEntity.ok(OperationStatus.SUCCESS);
  }

  /**
   * Creates a new user with the provided details.
   *
   * @param request the user details for creating a new user
   * @return if the operation is success
   */
  @Loggable
  @PostMapping
  @RequirePermission(autoDetect = true)
  public ApiResponse<?> createUser(@Valid @RequestBody UserRequest request) {
    // Password is required for creation
    if (request.getPassword() == null || request.getPassword().isBlank()) {
      // Generate a secure temporary password if not provided
      String tempPassword = userService.generateSecureTemporaryPassword();
      request.setPassword(tempPassword);
    }

    // Check if username or email already exists (regardless of enabled status)
    UserDto existingByUsername = userService.findByUsername(request.getUsername());
    UserDto existingByEmail = userService.findByEmail(request.getEmail());

    if (existingByUsername != null) {
      LOG.warn("Username already exists: {}", request.getUsername());
      return ApiResponse.error(HttpStatus.CONFLICT, UserConstants.EXIST_USERNAME, "");
    }

    if (existingByEmail != null) {
      LOG.warn("Email already exists: {}", request.getEmail());
      return ApiResponse.error(HttpStatus.CONFLICT, UserConstants.EXIST_EMAIL, "");
    }

    var userDto = UserUtils.convertToUserDto(request);

    var verificationToken = jwtService.generateJwtToken(userDto.getUsername());
    userDto.setVerificationToken(verificationToken);

    var savedUserDto = userService.createUser(userDto);

    var encryptedToken = encryptionService.encrypt(verificationToken);
    LOG.debug("Encrypted JWT token: {}", encryptedToken);
    var encodedToken = encryptionService.encode(encryptedToken);

    if (savedUserDto.getId() != null) {
      // Assign roles if provided
      if (request.getRoles() != null && !request.getRoles().isEmpty()) {
        try {
          userService.assignRolesToUser(savedUserDto.getId(), request.getRoles());
          LOG.info("Assigned {} roles to user {}", request.getRoles().size(), savedUserDto.getId());
        } catch (Exception e) {
          LOG.error("Failed to assign roles to user: {}", savedUserDto.getEmail(), e);
        }
      }

      // Generate new password for existing user
      String newPassword = userService.generateSecureTemporaryPassword();
      Boolean passwordUpdated = userService.updatePasswordDirectly(savedUserDto.getId(), newPassword);

      if (!passwordUpdated) {
        LOG.error("Failed to update password for user: {}", savedUserDto.getEmail());
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update password", "");
      }
      /*emailService.sendWelcomeEmail(savedUserDto,
          request.getPassword().length() > 0 ? request.getPassword() : newPassword);*/
    }
    return ApiResponse.success("", UserConstants.USER_CREATED_SUCCESS_MESSAGE, null);
  }

  /**
   * Updates an existing user with the provided details.
   * Note: Password updates should use the dedicated /update-password endpoint.
   *
   * @param id      the user Long to update
   * @param request the user details for updating
   * @return the updated user details
   */
  @Loggable
  @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @RequirePermission(autoDetect = true)
  public ApiResponse<?> updateUser(
      @PathVariable Long id,
      @Valid @RequestBody UserRequest request) {

    try {
      UserDto updatedUser = userService.updateUser(id, request);
      LOG.info("User with id {} updated successfully", id);
      return ApiResponse.success("", UserConstants.USER_UPDATED_SUCCESSFULLY, "");
    } catch (IllegalArgumentException e) {
      LOG.warn("Update failed for user {}: {}", id, e.getMessage());

      if (e.getMessage().equals(UserConstants.USER_NOT_FOUND)) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, e.getMessage(), "");
      } else if (e.getMessage().equals(UserConstants.EXIST_USERNAME) ||
                 e.getMessage().equals(UserConstants.EXIST_EMAIL)) {
        return ApiResponse.error(HttpStatus.CONFLICT, e.getMessage(), "");
      }
      return ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), "");
    } catch (Exception e) {
      LOG.error("Failed to update user with id: {}", id, e);
      return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, UserConstants.USER_UPDATE_FAILED, "");
    }
  }

  /**
   * Update the user password for the currently authenticated user.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return "Password updated successfully" if the password is updated
   */
  @Loggable
  @PostMapping(value = UserConstants.UPDATE_PASSWORD_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updatePassword(
      @RequestParam String oldPassword, @RequestParam String newPassword) {

    var userDetails = SecurityUtils.getAuthenticatedUserDetails();
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ErrorConstants.UNAUTHORIZED_ACCESS);
    }

    // Update the password
    String result = userService.updatePassword(userDetails.getId(), oldPassword, newPassword);

    return ResponseEntity.ok(result);
  }

  /**
   * Enables the user associated with the id.
   *
   * @param id the user Long
   * @return if the operation is success
   */
  @Loggable
  @PutMapping(value = UserConstants.ENABLE_USER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> enableUser(@PathVariable Long id) {
    var userDto = userService.enableUser(id);

    return ResponseEntity.ok(userDto == null ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Disables the user associated with the id.
   *
   * @param id the user Long
   * @return if the operation is success
   */
  @Loggable
  @PutMapping(value = UserConstants.DISABLE_USER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> disableUser(@PathVariable Long id) {
    var userDto = userService.disableUser(id);

    return ResponseEntity.ok(userDto == null ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

}
