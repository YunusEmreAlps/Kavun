package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.user.OtpService;
import com.kavun.backend.service.user.UserService;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.CookieService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.enums.OtpDeliveryMethod;
import com.kavun.enums.TokenType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.web.payload.request.ForgotPasswordRequest;
import com.kavun.web.payload.request.LoginRequest;
import com.kavun.web.payload.request.OtpVerificationRequest;
import com.kavun.web.payload.request.ResetPasswordRequest;
import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.AuthResponse;
import com.kavun.web.payload.response.LogoutResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

  private final OtpService otpService;
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
   * Attempts to authenticate with the provided credentials. If successful, an OTP is sent.
   * User must verify OTP in a separate request to receive JWT token.
   *
   * @param loginRequest the login request
   * @return response containing OTP id and masked target
   */
  /*@SecurityRequirements
  @Loggable(level = "debug")
  @PostMapping(value = SecurityConstants.LOGIN)
  public ApiResponse<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    String username = loginRequest.getUsername();
    // Authentication will fail if the credentials are invalid and throw exception.
    SecurityUtils.authenticateUser(authenticationManager, username, loginRequest.getPassword());

    // Find user and get OTP delivery method
    UserDto user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.error(HttpStatus.BAD_REQUEST, UserConstants.USER_NOT_FOUND, SecurityConstants.LOGIN);
    }

    // Check if user has OTP delivery method configured
    if (user.getOtpDeliveryMethod() == null || user.getOtpDeliveryMethod().isBlank()) {
      return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.BLANK_OTP_DELIVERY_METHOD, SecurityConstants.LOGIN);
    }

    // Generate and send OTP based on delivery method
    Map<String, Object> response;
    try {
      if (OtpDeliveryMethod.SMS.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpSms(user.getPhone());
      } else if (OtpDeliveryMethod.EMAIL.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpEmail(user.getEmail());
      } else {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.INVALID_OTP_DELIVERY_METHOD, SecurityConstants.LOGIN);
      }
      return ApiResponse.success(response, AuthConstants.OTP_SENT_SUCCESSFULLY, SecurityConstants.LOGIN);
    } catch (Exception e) {
      LOG.error("Failed to send OTP for user: {}", username, e);
      return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send OTP: " + e.getMessage(), SecurityConstants.LOGIN);
    }
  }*/

  /**
   * Endpoint to generate OTP for the user.
   *
   * @param username the username
   * @return the response entity
   */
  @SecurityRequirements
  @Loggable(level = "warn")
  @PostMapping(SecurityConstants.GENERATE_OTP)
  public ApiResponse<?> generateOtp(String username) {
    UserDto user = userService.findByUsername(username);

    if (user == null) {
      return ApiResponse.error(HttpStatus.BAD_REQUEST, UserConstants.USER_NOT_FOUND, SecurityConstants.GENERATE_OTP);
    }

    if (user.getOtpDeliveryMethod() == null || user.getOtpDeliveryMethod().isBlank()) {
      return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.USER_HAS_NO_OTP_DELIVERY_METHOD, SecurityConstants.GENERATE_OTP);
    }

    try {
      Map<String, Object> response;
      if (OtpDeliveryMethod.SMS.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpSms(user.getPhone());
      } else if (OtpDeliveryMethod.EMAIL.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpEmail(user.getEmail());
      } else {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.INVALID_OTP_DELIVERY_METHOD, SecurityConstants.GENERATE_OTP);
      }
      return ApiResponse.success(response, AuthConstants.OTP_SENT_SUCCESSFULLY, SecurityConstants.GENERATE_OTP);
    } catch (Exception e) {
      LOG.error("Failed to generate OTP for user: {}", username, e);
      return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate OTP: " + e.getMessage(), SecurityConstants.GENERATE_OTP);
    }
  }

  /**
   * Endpoint to verify OTP for the user and issue access token.
   *
   * @param refreshToken the refresh token (optional)
   * @param request the OTP verification request
   * @return JWT response with tokens and user details
   */
  @SecurityRequirements
  @Loggable(level = "warn")
  @PostMapping(SecurityConstants.VERIFY_OTP)
  public ResponseEntity<?> verifyOtp(
      @CookieValue(required = false) String refreshToken,
      @Valid @RequestBody OtpVerificationRequest request) {

    try {
      // Validate OTP - throws exception if invalid
      otpService.validateOtp(request.getId(), request.getTarget(), request.getCode());

      // Find user by target (email or phone)
      UserDto user = userService.findByPhone(request.getTarget());
      if (user == null) {
        user = userService.findByEmail(request.getTarget());
      }

      if (user == null) {
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, UserConstants.USER_NOT_FOUND, SecurityConstants.VERIFY_OTP));
      }

      // Authenticate user without password
      SecurityUtils.authenticateUser(userDetailsService.loadUserByUsername(user.getUsername()));

      // Check if refresh token is valid
      String decryptedRefreshToken = encryptionService.decrypt(refreshToken);
      boolean isRefreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);

      // Generate tokens and cookies
      HttpHeaders responseHeaders = new HttpHeaders();
      String newAccessToken = updateCookies(user.getUsername(), isRefreshTokenValid, responseHeaders);
      String encryptedAccessToken = encryptionService.encrypt(newAccessToken);

      // Convert expiration to seconds for OAuth2 compliance
      long expiresInSeconds = (long) accessTokenExpirationInMinutes * 60;

      // Build response with user details
      UserDetailsBuilder userDetails = (UserDetailsBuilder) userDetailsService.loadUserByUsername(user.getUsername());
      AuthResponse authResponse = AuthResponse.of(encryptedAccessToken, expiresInSeconds, null, userDetails);

      return ResponseEntity.ok().headers(responseHeaders).body(ApiResponse.success(authResponse, AuthConstants.OTP_VERIFIED, SecurityConstants.VERIFY_OTP));

    } catch (IllegalArgumentException e) {
      // OTP validation failed with specific error message
      return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), SecurityConstants.VERIFY_OTP));
    } catch (Exception e) {
      LOG.error("OTP verification failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "OTP verification failed: " + e.getMessage(), SecurityConstants.VERIFY_OTP));
    }
  }

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
      Boolean passwordUpdated = userService.updatePasswordDirectly(user.getId(), newPassword);

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
