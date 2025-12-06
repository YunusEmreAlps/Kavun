package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.user.UserService;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.CookieService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.enums.TokenType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.web.payload.request.ForgotPasswordRequest;
import com.kavun.web.payload.request.LoginRequest;
import com.kavun.web.payload.request.ResetPasswordRequest;
import com.kavun.web.payload.response.AuthResponse;
import com.kavun.web.payload.response.LogoutResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class attempt to authenticate with AuthenticationManager bean, add an
 * authentication object
 * to SecurityContextHolder then Generate JWT token, then return JWT to a
 * client.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_V1_AUTH_ROOT_URL)
@Tag(name = "01. Authentication", description = "APIs for user authentication and authorization")
public class AuthRestApi {

  @Value("${access-token-expiration-in-minutes}")
  private int accessTokenExpirationInMinutes;

  private final JwtService jwtService;
  private final UserService userService;
  private final EmailService emailService;
  private final CookieService cookieService;
  private final EncryptionService encryptionService;
  private final UserDetailsService userDetailsService;
  private final DaoAuthenticationProvider authenticationManager;

  /**
   * Attempts to authenticate with the provided credentials. If successful, a JWT
   * token is returned
   * with some user details.
   *
   * <p>
   * A refresh token is generated and returned as a cookie.
   *
   * @param refreshToken The refresh token
   * @param loginRequest the login request
   * @return the jwt token details
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(value = SecurityConstants.LOGIN)
  public ResponseEntity<AuthResponse> authenticateUser(
      @CookieValue(required = false) String refreshToken,
      @Valid @RequestBody LoginRequest loginRequest) {

    var username = loginRequest.getUsername();
    // Authentication will fail if the credentials are invalid and throw exception.
    SecurityUtils.authenticateUser(authenticationManager, username, loginRequest.getPassword());

    var decryptedRefreshToken = encryptionService.decrypt(refreshToken);
    var isRefreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);

    var responseHeaders = new HttpHeaders();
    // If the refresh token is valid, then we will not generate a new refresh token.
    String newAccessToken = updateCookies(username, isRefreshTokenValid, responseHeaders);
    String encryptedAccessToken = encryptionService.encrypt(newAccessToken);

    // Convert expiration to seconds for OAuth2 compliance
    long expiresInSeconds = (long) accessTokenExpirationInMinutes * 60;

    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(AuthResponse.of(encryptedAccessToken, expiresInSeconds, null, null));
  }

  /**
   * Attempts to authenticate with the provided credentials. If successful, a JWT
   * token is returned
   * with some user details. (With OTP)
   *
   * <p>
   * A refresh token is generated and returned as a cookie.
   *
   * @param refreshToken The refresh token
   * @param loginRequest the login request
   * @return the jwt token details
   */
  /*
   * @SecurityRequirements
   *
   * @Loggable(level = "debug")
   *
   * @PostMapping(value = SecurityConstants.LOGIN)
   * public ResponseEntity<?> authenticateUser(
   *
   * @CookieValue(required = false) String refreshToken,
   *
   * @Valid @RequestBody LoginRequest loginRequest) {
   *
   * var username = loginRequest.getUsername();
   * // Authentication will fail if the credentials are invalid and throw
   * exception.
   * SecurityUtils.authenticateUser(authenticationManager, username,
   * loginRequest.getPassword());
   *
   * // Generate and Send OTP
   * String target = null;
   * UserDto user = userService.findByUsername(username);
   * if (user != null) {
   * if (user.getOtpDeliveryMethod().equals(OtpDeliveryMethod.SMS.name())) {
   * target = user.getPhone();
   * } else if
   * (user.getOtpDeliveryMethod().equals(OtpDeliveryMethod.EMAIL.name())) {
   * target = user.getEmail();
   * }
   * }
   * if (target != null) {
   * CustomResponse<Object> response = otpService.generateOtp(target);
   * if (user.getOtpDeliveryMethod().equals(OtpDeliveryMethod.SMS.name())) {
   * // TODO: Send the OTP code to the phone number
   * } else if
   * (user.getOtpDeliveryMethod().equals(OtpDeliveryMethod.EMAIL.name())) {
   * emailService.sendOtpEmail(user, response.getData().get().toString());
   * } else {
   * return ResponseEntity.status(HttpStatus.BAD_REQUEST)
   * .body(
   * CustomResponse.of(
   * HttpStatus.BAD_REQUEST.value(),
   * null,
   * AuthConstants.INVALID_OTP_DELIVERY_METHOD,
   * SecurityConstants.LOGIN));
   * }
   * return ResponseEntity.ok(response);
   * }
   * return ResponseEntity.status(HttpStatus.BAD_REQUEST)
   * .body(
   * CustomResponse.of(
   * HttpStatus.BAD_REQUEST.value(),
   * null,
   * AuthConstants.BLANK_OTP_DELIVERY_METHOD,
   * SecurityConstants.LOGIN));
   * }
   */

  /**
   * Refreshes the current access token and refresh token accordingly.
   *
   * @param refreshToken The refresh token
   * @param request      The request
   * @return the jwt token details
   */
  @Loggable
  @SecurityRequirements
  @GetMapping(value = SecurityConstants.REFRESH_TOKEN)
  public ResponseEntity<AuthResponse> refreshToken(
      @CookieValue String refreshToken, HttpServletRequest request) {

    var decryptedRefreshToken = encryptionService.decrypt(refreshToken);
    boolean refreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);

    if (!refreshTokenValid) {
      throw new IllegalArgumentException(ErrorConstants.INVALID_TOKEN);
    }
    var username = jwtService.getUsernameFromToken(decryptedRefreshToken);
    var userDetails = userDetailsService.loadUserByUsername(username);

    SecurityUtils.validateUserDetailsStatus(userDetails);
    SecurityUtils.authenticateUser(request, userDetails);

    var expiration = DateUtils.addMinutes(new Date(), accessTokenExpirationInMinutes);
    var newAccessToken = jwtService.generateJwtToken(username, expiration);
    var encryptedAccessToken = encryptionService.encrypt(newAccessToken);

    // Cast to UserDetailsBuilder for AuthResponse
    var userDetailsBuilder = (UserDetailsBuilder) userDetails;

    return ResponseEntity.ok(
        AuthResponse.of(encryptedAccessToken, accessTokenExpirationInMinutes * 60L, null, userDetailsBuilder));
  }

  /**
   * Endpoint to generate OTP for the user.
   *
   * @param username the username
   * @return the response entity
   */
  /*
   * @SecurityRequirements
   *
   * @Loggable(level = "warn")
   *
   * @PostMapping(SecurityConstants.GENERATE_OTP)
   * public ResponseEntity<CustomResponse<Object>> generateOtp(String username) {
   * UserDto user = userService.findByUsername(username);
   * String target = null;
   *
   * if (user.getOtpDeliveryMethod() == OtpDeliveryMethod.SMS.name()) {
   * target = user.getPhone();
   * } else if (user.getOtpDeliveryMethod() == OtpDeliveryMethod.EMAIL.name()) {
   * target = user.getEmail();
   * }
   *
   * // Get the user email or phone number from the username
   * if (target == null) {
   * return ResponseEntity.status(HttpStatus.BAD_REQUEST)
   * .body(
   * CustomResponse.of(
   * HttpStatus.BAD_REQUEST.value(),
   * null,
   * UserConstants.USER_NOT_FOUND,
   * SecurityConstants.GENERATE_OTP));
   * } else {
   * CustomResponse<Object> response = otpService.generateOtp(target);
   * if (user.getOtpDeliveryMethod() == OtpDeliveryMethod.SMS.name()) {
   * // TODO: Send the OTP code to the phone number
   * } else if (user.getOtpDeliveryMethod() == OtpDeliveryMethod.EMAIL.name()) {
   * emailService.sendOtpEmail(user, response.getData().get().toString());
   * }
   * return ResponseEntity.ok(response);
   * }
   * }
   */

  /**
   * Endpoint to verify OTP for the user.
   *
   * @param request the OTP verification request
   * @return the response entity
   */
  /*
   * @SecurityRequirements
   *
   * @Loggable(level = "warn")
   *
   * @PostMapping(SecurityConstants.VERIFY_OTP)
   * public ResponseEntity<CustomResponse<?>> verifyOtp(
   *
   * @CookieValue(required = false) String refreshToken,
   *
   * @RequestBody OtpVerificationRequest request) {
   * CustomResponse<Boolean> response =
   * otpService.validateOtp(request.getPublicId(), request.getTarget(),
   * request.getCode());
   * if (response.getData().orElse(false)) {
   *
   * UserDto user = userService.findByEmail(request.getTarget());
   * if (user == null) {
   * user = userService.findByPhone(request.getTarget());
   * }
   *
   * if (user == null) {
   * return ResponseEntity.status(HttpStatus.BAD_REQUEST)
   * .body(
   * CustomResponse.of(
   * HttpStatus.BAD_REQUEST.value(),
   * null,
   * UserConstants.USER_NOT_FOUND,
   * SecurityConstants.VERIFY_OTP));
   * }
   *
   * var decryptedRefreshToken = encryptionService.decrypt(refreshToken);
   * var isRefreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);
   *
   * // Authenticate user without password by setting it in the
   * SecurityContextHolder
   * SecurityUtils.authenticateUser(userDetailsService.loadUserByUsername(user.
   * getUsername()));
   *
   * var responseHeaders = new HttpHeaders();
   *
   * // If the refresh token is valid, then we will not generate a new refresh
   * token.
   * String newAccessToken = updateCookies(user.getUsername(),
   * isRefreshTokenValid, responseHeaders);
   * String encryptedAccessToken = encryptionService.encrypt(newAccessToken);
   *
   * JwtResponseBuilder jwtResponse =
   * JwtResponseBuilder.buildJwtResponse(encryptedAccessToken);
   *
   * return ResponseEntity.ok()
   * .headers(responseHeaders)
   * .body(
   * CustomResponse.of(
   * HttpStatus.OK.value(),
   * jwtResponse,
   * AuthConstants.OTP_VERIFIED,
   * SecurityConstants.VERIFY_OTP));
   * } else {
   * return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
   * }
   * }
   */

  /**
   * Endpoint to handle forgot password requests.
   * Generates a new password and sends it directly to user's email.
   *
   * @param request the forgot password request
   * @return response entity
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(SecurityConstants.FORGOT_PASSWORD)
  public ResponseEntity<String> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    // Process async for security (prevent timing attacks)
    CompletableFuture.runAsync(() -> processForgotPassword(request));

    // Always return success to prevent user enumeration
    return buildSuccessResponse();
  }

  /**
   * Process forgot password request asynchronously.
   */
  private void processForgotPassword(ForgotPasswordRequest request) {
    try {
      UserDto user = findUserByEmailOrUsername(request);

      if (user == null) {
        LOG.debug("Password reset requested for non-existent user");
        return;
      }

      String newPassword = userService.generateSecureTemporaryPassword();
      Boolean passwordUpdated = userService.updatePasswordDirectly(user.getPublicId(), newPassword);

      if (!passwordUpdated) {
        LOG.error("Failed to update password for user: {}", user.getEmail());
        return;
      }

      // TODO: Send new password via email
      // emailService.sendPasswordResetEmail(user, newPassword);

      LOG.info("Password reset completed for user: {}", user.getEmail());
    } catch (Exception e) {
      LOG.error("Error processing forgot password request", e);
    }
  }

  /**
   * Find user by email or username from the request.
   */
  private UserDto findUserByEmailOrUsername(ForgotPasswordRequest request) {
    String email = request.getEmail();
    String username = request.getUsername();

    if (email != null && !email.isBlank()) {
      UserDto user = userService.findByEmail(email.trim().toLowerCase());
      if (user != null) {
        return user;
      }
    }

    if (username != null && !username.isBlank()) {
      return userService.findByUsername(username.trim().toLowerCase());
    }

    return null;
  }

  /**
   * Endpoint to handle password reset requests.
   *
   * @param request the reset password request
   * @return response entity
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(SecurityConstants.RESET_PASSWORD)
  public ResponseEntity<String> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    return ResponseEntity.ok(
        userService.resetPassword(request.getToken(), request.getNewPassword()));
  }

  /**
   * Logout the user from the system and clear all cookies from request and
   * response.
   *
   * @param request  the request
   * @param response the response
   * @return response entity
   */
  @Loggable
  @SecurityRequirements
  @DeleteMapping(value = SecurityConstants.LOGOUT)
  public ResponseEntity<LogoutResponse> logout(
      HttpServletRequest request, HttpServletResponse response) {
    SecurityUtils.logout(request, response);

    var responseHeaders = cookieService.addDeletedCookieToHeaders(TokenType.REFRESH);
    var logoutResponse = new LogoutResponse(OperationStatus.SUCCESS);
    SecurityUtils.clearAuthentication();

    return ResponseEntity.ok().headers(responseHeaders).body(logoutResponse);
  }

  // =========================================================================
  // UTILITY OPERATIONS
  // =========================================================================

  /**
   * Helper method to build consistent success response.
   */
  private ResponseEntity<String> buildSuccessResponse() {
    return ResponseEntity.ok(UserConstants.PASSWORD_RESET_EMAIL_SENT_SUCCESSFULLY);
  }

  /**
   * Creates a refresh token if expired and adds it to the cookies.
   *
   * @param username       the username
   * @param isRefreshValid if the refresh token is valid
   * @param headers        the http headers
   */
  private String updateCookies(
      String username, boolean isRefreshValid, MultiValueMap<String, String> headers) {

    if (!isRefreshValid) {
      var token = jwtService.generateJwtToken(username);
      var refreshDuration = Duration.ofDays(SecurityConstants.DEFAULT_TOKEN_DURATION);

      var encryptedToken = encryptionService.encrypt(token);
      cookieService.addCookieToHeaders(
          (HttpHeaders) headers, TokenType.REFRESH, encryptedToken, refreshDuration);
    }

    var accessTokenExpiration = DateUtils.addMinutes(new Date(), accessTokenExpirationInMinutes);
    return jwtService.generateJwtToken(username, accessTokenExpiration);
  }
}
