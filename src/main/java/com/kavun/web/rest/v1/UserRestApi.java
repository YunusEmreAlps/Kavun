package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
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
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.request.SignUpRequest;
import com.kavun.web.payload.response.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.Objects;
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

  private static final String AUTHORIZE = "isFullyAuthenticated() && hasRole(T(com.kavun.enums.RoleType).ROLE_ADMIN)";

  /**
   * Searches for users based on the provided parameters
   *
   * @param paramaterMap a map of search parameters where the key is the
   * @param page         pagination information
   * @return a paginated list of users that match the search criteria
   */
  @Loggable
  @PageableAsQueryParam
  @PreAuthorize(AUTHORIZE)
  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<UserResponse>> searchUsers(
      @RequestParam Map<String, Object> paramaterMap,
      Pageable page) {

    Specification<User> spec = userSpecification.search(paramaterMap);

    Page<UserResponse> users = userService.findAll(spec, page);
    return ResponseEntity.ok(users);
  }

  /**
   * Performs a search for users based on the provided search criteria.
   *
   * @param page Allows for pagination of the search results.
   * @return The ResponseEntity containing the search results as a Page of users
   */
  @Loggable
  @PageableAsQueryParam
  @PreAuthorize(AUTHORIZE)
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<UserResponse>> getUsers(final Pageable page) {

    Page<UserResponse> users = userService.findAll(page);
    return ResponseEntity.ok(users);
  }

  /**
   * Deletes the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @Loggable
  @PreAuthorize(AUTHORIZE)
  @DeleteMapping(value = "/{publicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> deleteUser(@PathVariable String publicId) {
    userService.deleteUser(publicId);

    return ResponseEntity.ok(OperationStatus.SUCCESS);
  }

  /**
   * Creates a new user.
   *
   * @param signUpRequest the user details
   * @return ResponseEntity with location header of the created user
   */
  @Loggable
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<String> createUser(@Valid @RequestBody SignUpRequest signUpRequest) {
    var userDto = UserUtils.convertToUserDto(signUpRequest);

    if (userService.existsByUsernameOrEmailAndEnabled(userDto.getUsername(), userDto.getEmail())) {
      LOG.warn(UserConstants.USERNAME_OR_EMAIL_EXISTS);
      return ResponseEntity.badRequest().body(UserConstants.USERNAME_OR_EMAIL_EXISTS);
    }

    var verificationToken = jwtService.generateJwtToken(userDto.getUsername());
    userDto.setVerificationToken(verificationToken);

    var savedUserDto = userService.createUser(userDto);

    var encryptedToken = encryptionService.encrypt(verificationToken);
    LOG.debug("Encrypted JWT token: {}", encryptedToken);
    var encodedToken = encryptionService.encode(encryptedToken);

    emailService.sendAccountVerificationEmail(savedUserDto, encodedToken);
    var location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{publicId}")
        .buildAndExpand(savedUserDto.getPublicId())
        .toUriString();

    return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, location).build();
  }

  /**
   * Update the user password for the currently authenticated user.
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   * @return "Password updated successfully" if the password is updated
   */
  @Loggable
  @PreAuthorize(AUTHORIZE)
  @PostMapping(value = UserConstants.UPDATE_PASSWORD_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updatePassword(
      @RequestParam String oldPassword, @RequestParam String newPassword) {

    var userDetails = SecurityUtils.getAuthenticatedUserDetails();
    if (Objects.isNull(userDetails)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ErrorConstants.UNAUTHORIZED_ACCESS);
    }

    // Update the password
    String result = userService.updatePassword(userDetails.getPublicId(), oldPassword, newPassword);

    return ResponseEntity.ok(result);
  }

  /**
   * Enables the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @Loggable
  @PreAuthorize(AUTHORIZE)
  @PutMapping(value = "/{publicId}/enable")
  public ResponseEntity<OperationStatus> enableUser(@PathVariable String publicId) {
    var userDto = userService.enableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Disables the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @Loggable
  @PreAuthorize(AUTHORIZE)
  @PutMapping(value = "/{publicId}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> disableUser(@PathVariable String publicId) {
    var userDto = userService.disableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

}
