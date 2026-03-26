package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.user.OtpService;
import com.kavun.backend.service.user.UserDeviceService;
import com.kavun.backend.service.user.UserService;
import com.kavun.backend.service.user.UserSessionService;
import com.kavun.backend.service.mail.EmailService;
import com.kavun.backend.service.security.CookieService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.LoggingConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.enums.OtpDeliveryMethod;
import com.kavun.enums.TokenType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.CaptchaGenerator;
import com.kavun.shared.util.CaptchaStore;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.backend.persistent.domain.user.Captcha;
import com.kavun.backend.persistent.domain.user.UserSession;
import com.kavun.backend.persistent.repository.CaptchaRepository;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.kavun.web.payload.response.CaptchaResponse;


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

  @Value("${login.otp.enabled:false}")
  private boolean isOtpEnabled;

  @Value("${login.captcha.enabled:false}")
  private boolean isCaptchaEnabled;

  private final OtpService otpService;
  private final JwtService jwtService;
  private final UserService userService;
  private final EmailService emailService;
  private final CookieService cookieService;
  private final EncryptionService encryptionService;
  private final UserDetailsService userDetailsService;
  private final UserSessionService userSessionService;
  private final UserDeviceService userDeviceService;
  private final DaoAuthenticationProvider authenticationManager;

  private final CaptchaRepository captchaRepository;

  // Rate limiting caches
  private final ConcurrentHashMap<String, Bucket> captchaRateLimitCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Bucket> loginRateLimitCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Bucket> forgotPasswordIpCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Bucket> forgotPasswordUserCache = new ConcurrentHashMap<>();

  /**
   * Generates a new CAPTCHA image and unique ID.
   * Implements rate limiting (5 requests per minute per IP) to prevent abuse.
   */
  @Loggable
  @SecurityRequirements
  @GetMapping("/captcha")
  public ApiResponse<CaptchaResponse> getCaptcha(HttpServletRequest request) {

    // Rate limiting - 5 captchas per minute per IP
    Bucket bucket = resolveCaptchaBucket(request);
    if (!bucket.tryConsume(1)) {
      // Calculate seconds until next token is available
      long nanosUntilRefill = bucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill();
      long secondsToWait = Duration.ofNanos(nanosUntilRefill).getSeconds();

      LOG.warn("CAPTCHA rate limit exceeded for IP: {}. Retry after {} seconds",
          request.getRemoteAddr(), secondsToWait);
      return ApiResponse.error(
          HttpStatus.TOO_MANY_REQUESTS,
          String.format(AuthConstants.TOO_MANY_CAPTCHA_REQUESTS,
              secondsToWait),
          SecurityConstants.LOGIN);
    }

    try {
      // Generate captcha code and image
      String code = CaptchaGenerator.generateCode(5);
      String imageBase64 = CaptchaGenerator.generateImageBase64(code);
      String captchaId = CaptchaStore.saveCaptcha(code);

      CaptchaResponse response = new CaptchaResponse(imageBase64, captchaId);

      // Save captcha to database for persistence
      Captcha captchaEntity = new Captcha();
      captchaEntity.setCaptchaId(captchaId);
      captchaEntity.setCode(code);
      captchaEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
      captchaEntity.setUsed(false);
      captchaEntity.setUsedAt(null);
      captchaEntity.setIpAddress(request.getRemoteAddr());
      captchaRepository.save(captchaEntity);

      return ApiResponse.success(
          response,
          null,
          SecurityConstants.LOGIN);

    } catch (Exception e) {
      LOG.error("Error generating CAPTCHA for IP: {}", request.getRemoteAddr(), e);
      return ApiResponse.error(
          HttpStatus.INTERNAL_SERVER_ERROR,
          AuthConstants.CAPTCHA_CREATION_FAILED,
          SecurityConstants.LOGIN);
    }
  }

  /**
   * Creates or retrieves rate limiting bucket for CAPTCHA generation.
   * Allows 5 requests per minute per IP address.
   */
  private Bucket resolveCaptchaBucket(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    return captchaRateLimitCache.computeIfAbsent(ip, k -> {
      // 5 captcha requests per minute per IP
      Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5,
          Duration.ofMinutes(1)));
      return Bucket.builder().addLimit(limit).build();
    });
  }

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
  public ApiResponse<?> authenticateUser(@CookieValue(required = false) String refreshToken,
      @Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {

    var username = loginRequest.getUsername();

    if (isCaptchaEnabled) {
      // Step 1: Rate limiting for login attempts (5 per minute per IP)
      Bucket loginBucket = resolveLoginBucket(request);
      if (!loginBucket.tryConsume(1)) {
        LOG.warn("Login rate limit exceeded for IP: {}, Username: {}",
            request.getRemoteAddr(), username);
        return ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS,
            AuthConstants.TOO_MANY_LOGIN_ATTEMPTS, SecurityConstants.LOGIN);
      }

      // Step 2: Validate CAPTCHA
      Captcha captcha = captchaRepository.findValidCaptcha(
          loginRequest.getCaptchaId(),
          loginRequest.getCaptchaText(),
          LocalDateTime.now());

      // is CAPTCHA valid and not reused from database
      if (captcha == null) {
        LOG.warn("Invalid or reused CAPTCHA for user: {} from IP: {}", username,
            request.getRemoteAddr());
        return ApiResponse.error(HttpStatus.BAD_REQUEST,
            AuthConstants.INVALID_CAPTCHA, SecurityConstants.LOGIN);
      }

      // Step 3: Mark CAPTCHA as used to prevent reuse
      captcha.setUsed(true);
      captcha.setUsedAt(LocalDateTime.now());
      captchaRepository.save(captcha);
    }

    UserDto user = userService.findByUsername(username);
    if (user == null) {
      return ApiResponse.error(HttpStatus.BAD_REQUEST,
          UserConstants.USER_NOT_FOUND, SecurityConstants.LOGIN);
    }

    try {
      // Authentication will fail if the credentials are invalid
      SecurityUtils.authenticateUser(authenticationManager, username,
          loginRequest.getPassword());
      LOG.info("User {} authenticated successfully from IP: {}", username, request.getRemoteAddr());
      registerDeviceId(user.getId(), request);
    } catch (Exception e) {
      LOG.warn("Authentication failed for user: {}", username);
      return ApiResponse.error(HttpStatus.UNAUTHORIZED, AuthConstants.INVALID_CREDENTIALS, SecurityConstants.LOGIN);
    }

    var decryptedRefreshToken = encryptionService.decrypt(refreshToken);
    var isRefreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);

    if (isOtpEnabled) {
      // Check if user has OTP delivery method configured
      if (user.getOtpDeliveryMethod() == null ||
          user.getOtpDeliveryMethod().isBlank()) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST,
            AuthConstants.BLANK_OTP_DELIVERY_METHOD, SecurityConstants.LOGIN);
      }

      // Generate and send OTP based on delivery method
      Map<String, Object> response;
      try {
        if (OtpDeliveryMethod.SMS.name().equals(user.getOtpDeliveryMethod())) {
          if (user.getPhone() == null || user.getPhone().isBlank()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST,
                AuthConstants.USER_HAS_NO_PHONE_FOR_OTP, SecurityConstants.LOGIN);
          }
          response = otpService.generateAndSendOtpSms(user.getPhone());
        } else if (OtpDeliveryMethod.EMAIL.name().equals(user.getOtpDeliveryMethod())) {
          if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST,
                AuthConstants.USER_HAS_NO_EMAIL_FOR_OTP, SecurityConstants.LOGIN);
          }
          response = otpService.generateAndSendOtpEmail(user.getEmail());
        } else {
          return ApiResponse.error(HttpStatus.BAD_REQUEST,
              AuthConstants.INVALID_OTP_DELIVERY_METHOD, SecurityConstants.LOGIN);
        }
        return ApiResponse.success(response, AuthConstants.OTP_SENT_SUCCESSFULLY,
            SecurityConstants.LOGIN);
      } catch (Exception e) {
        LOG.error("Failed to send OTP for user: {}", username, e);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to send OTP: " + e.getMessage(), SecurityConstants.LOGIN);
      }
    } else {

      // Create session FIRST to get session ID
      UserSession session = userSessionService.createSession(user.getId(), request);
      String sessionId = session.getId().toString();
      LOG.info("Created session: {} for user: {}", sessionId, username);

      var responseHeaders = new HttpHeaders();
      // Generate access token with session ID embedded in JWT
      var accessTokenExpiration = DateUtils.addMinutes(new Date(), accessTokenExpirationInMinutes);
      String newAccessToken = jwtService.generateJwtToken(username, accessTokenExpiration, sessionId);

      // Handle refresh token
      if (!isRefreshTokenValid) {
        var refreshExpiration = DateUtils.addDays(new Date(), SecurityConstants.DEFAULT_TOKEN_DURATION);
        var newRefreshToken = jwtService.generateJwtToken(username, refreshExpiration, sessionId);
        var encryptedRefreshToken = encryptionService.encrypt(newRefreshToken);
        var refreshDuration = Duration.ofDays(SecurityConstants.DEFAULT_TOKEN_DURATION);
        cookieService.addCookieToHeaders(responseHeaders, TokenType.REFRESH, encryptedRefreshToken, refreshDuration);
      }

      String encryptedAccessToken = encryptionService.encrypt(newAccessToken);

      // Convert expiration to seconds for OAuth2 compliance
      long expiresInSeconds = (long) accessTokenExpirationInMinutes * 60;

      return ApiResponse.success(
          AuthResponse.of(encryptedAccessToken, expiresInSeconds, null, null, sessionId),
          null,
          SecurityConstants.LOGIN);
    }
  }

  /**
   * Rate limiting bucket for login attempts (5 per minute per IP).
   * Stricter than captcha generation to prevent credential stuffing.
   *
   */
  private Bucket resolveLoginBucket(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    return loginRateLimitCache.computeIfAbsent(ip, k -> {
      // 5 login attempts per minute per IP
      Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5,
          Duration.ofMinutes(1)));
      return Bucket.builder().addLimit(limit).build();
    });
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

    // Extract session ID from refresh token
    String sessionId = jwtService.getSessionIdFromToken(decryptedRefreshToken);

    var userDetails = userDetailsService.loadUserByUsername(username);

    SecurityUtils.validateUserDetailsStatus(userDetails);
    SecurityUtils.authenticateUser(request, userDetails);

    // Update session activity if session ID is present
    if (sessionId != null && !sessionId.isBlank()) {
      try {
        userSessionService.updateActivity(Long.parseLong(sessionId));
        LOG.debug("Updated activity for session: {}", sessionId);
      } catch (Exception e) {
        LOG.warn("Failed to update session activity: {}", e.getMessage());
      }
    }

    var expiration = DateUtils.addMinutes(new Date(), accessTokenExpirationInMinutes);
    // Generate new access token with the same session ID
    var newAccessToken = jwtService.generateJwtToken(username, expiration, sessionId);
    var encryptedAccessToken = encryptionService.encrypt(newAccessToken);

    // Cast to UserDetailsBuilder for AuthResponse
    var userDetailsBuilder = (UserDetailsBuilder) userDetails;

    return ResponseEntity.ok(
        AuthResponse.of(encryptedAccessToken, accessTokenExpirationInMinutes * 60L, null, userDetailsBuilder,
            sessionId));
  }

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
      return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.USER_HAS_NO_OTP_DELIVERY_METHOD,
          SecurityConstants.GENERATE_OTP);
    }

    try {
      Map<String, Object> response;
      if (OtpDeliveryMethod.SMS.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpSms(user.getPhone());
      } else if (OtpDeliveryMethod.EMAIL.name().equals(user.getOtpDeliveryMethod())) {
        response = otpService.generateAndSendOtpEmail(user.getEmail());
      } else {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, AuthConstants.INVALID_OTP_DELIVERY_METHOD,
            SecurityConstants.GENERATE_OTP);
      }
      return ApiResponse.success(response, AuthConstants.OTP_SENT_SUCCESSFULLY, SecurityConstants.GENERATE_OTP);
    } catch (Exception e) {
      LOG.error("Failed to generate OTP for user: {}", username, e);
      return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate OTP: " + e.getMessage(),
          SecurityConstants.GENERATE_OTP);
    }
  }

  /**
   * Endpoint to verify OTP for the user and issue access token.
   *
   * @param refreshToken the refresh token (optional)
   * @param request      the OTP verification request
   * @return JWT response with tokens and user details
   */
  @SecurityRequirements
  @Loggable(level = "warn")
  @PostMapping(SecurityConstants.VERIFY_OTP)
  public ResponseEntity<?> verifyOtp(
      @CookieValue(required = false) String refreshToken,
      @Valid @RequestBody OtpVerificationRequest request,
      HttpServletRequest httpRequest) {

    try {
      // Validate OTP - throws exception if invalid
      otpService.validateOtp(request.getId(), request.getTarget(), request.getCode());

      // Find user by target (email or phone)
      UserDto user = userService.findByPhone(request.getTarget());
      if (user == null) {
        user = userService.findByEmail(request.getTarget());
      }

      if (user == null) {
        return ResponseEntity.badRequest().body(
            ApiResponse.error(HttpStatus.BAD_REQUEST, UserConstants.USER_NOT_FOUND, SecurityConstants.VERIFY_OTP));
      }

      // Authenticate user without password
      SecurityUtils.authenticateUser(userDetailsService.loadUserByUsername(user.getUsername()));

      // Create session and get the session ID
      var userSession = userSessionService.createSession(user.getId(), httpRequest);
      String sessionId = userSession.getId().toString();
      LOG.info("Created session: {} for user: {} after OTP verification", sessionId, user.getUsername());

      // Check if refresh token is valid
      String decryptedRefreshToken = encryptionService.decrypt(refreshToken);
      boolean isRefreshTokenValid = jwtService.isValidJwtToken(decryptedRefreshToken);

      // Generate tokens with session ID embedded
      HttpHeaders responseHeaders = new HttpHeaders();
      var accessTokenExpiration = DateUtils.addMinutes(new Date(), accessTokenExpirationInMinutes);
      String newAccessToken = jwtService.generateJwtToken(user.getUsername(), accessTokenExpiration, sessionId);

      // Handle refresh token
      if (!isRefreshTokenValid) {
        var refreshExpiration = DateUtils.addDays(new Date(), SecurityConstants.DEFAULT_TOKEN_DURATION);
        var newRefreshToken = jwtService.generateJwtToken(user.getUsername(), refreshExpiration, sessionId);
        var encryptedRefreshToken = encryptionService.encrypt(newRefreshToken);
        var refreshDuration = Duration.ofDays(SecurityConstants.DEFAULT_TOKEN_DURATION);
        cookieService.addCookieToHeaders(responseHeaders, TokenType.REFRESH, encryptedRefreshToken, refreshDuration);
      }

      String encryptedAccessToken = encryptionService.encrypt(newAccessToken);

      // Convert expiration to seconds for OAuth2 compliance
      long expiresInSeconds = (long) accessTokenExpirationInMinutes * 60;

      // Build response with user details
      UserDetailsBuilder userDetails = (UserDetailsBuilder) userDetailsService.loadUserByUsername(user.getUsername());
      AuthResponse authResponse = AuthResponse.of(encryptedAccessToken, expiresInSeconds, null, userDetails, sessionId);

      return ResponseEntity.ok().headers(responseHeaders)
          .body(ApiResponse.success(authResponse, AuthConstants.OTP_VERIFIED, SecurityConstants.VERIFY_OTP));

    } catch (IllegalArgumentException e) {
      // OTP validation failed with specific error message
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), SecurityConstants.VERIFY_OTP));
    } catch (Exception e) {
      LOG.error("OTP verification failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "OTP verification failed: " + e.getMessage(),
              SecurityConstants.VERIFY_OTP));
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
  public ApiResponse<Object> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {

    // IP-based rate limiting
    Bucket ipBucket = resolveForgotPasswordIpBucket(httpRequest);
    if (!ipBucket.tryConsume(1)) {
      long nanosUntilRefill = ipBucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill();
      long minutesToWait = Duration.ofNanos(nanosUntilRefill).toMinutes() + 1;

      return ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS,
          String.format("Çok fazla şifre sıfırlama isteği. Lütfen %d dakika bekleyin.", minutesToWait),
          SecurityConstants.FORGOT_PASSWORD);
    }

    // User-based rate limiting
    Bucket userBucket = resolveForgotPasswordUserBucket(
        request.getEmail() != null ? request.getEmail() : request.getUsername());
    if (!userBucket.tryConsume(1)) {
      long nanosUntilRefill = userBucket.tryConsumeAndReturnRemaining(1).getNanosToWaitForRefill();
      long minutesToWait = Duration.ofNanos(nanosUntilRefill).toMinutes() + 1;

      return ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS,
          String.format(AuthConstants.TOO_MANY_PASSWORD_RESET_REQUESTS, minutesToWait),
          SecurityConstants.FORGOT_PASSWORD);
    }

    // Process async for security (prevent timing attacks)
    CompletableFuture.runAsync(() -> processForgotPassword(request));

    return ApiResponse.success(UserConstants.PASSWORD_RESET_EMAIL_SENT_SUCCESSFULLY, SecurityConstants.FORGOT_PASSWORD);
  }

  // Process forgot password request asynchronously.
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

      // Send new password via email
      emailService.sendPasswordResetEmail(user, newPassword);

      LOG.info("Password reset completed for user: {}", user.getEmail());
    } catch (Exception e) {
      LOG.error("Error processing forgot password request", e);
    }
  }

  // LAYER 1: IP-based (prevents DDoS/mass attacks)
  private Bucket resolveForgotPasswordIpBucket(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    return forgotPasswordIpCache.computeIfAbsent(ip, k -> {
      // 3 requests per HOUR per IP
      Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3,
          Duration.ofHours(1)));
      return Bucket.builder().addLimit(limit).build();
    });
  }

  // LAYER 2: User-based (prevents account enumeration)

  private Bucket resolveForgotPasswordUserBucket(String identifier) {
    return forgotPasswordUserCache.computeIfAbsent(identifier.toLowerCase(), k -> {
      // 3 requests per 15 MINUTES per email/username
      Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3,
          Duration.ofMinutes(15)));
      return Bucket.builder().addLimit(limit).build();
    });
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
   * <p>
   * Session ID is extracted from the JWT token for security. If present,
   * the corresponding UserSession is marked as logged out in the database.
   *
   * @param request  the request
   * @param response the response
   * @return response entity
   */
  @Loggable
  @DeleteMapping(value = SecurityConstants.LOGOUT)
  public ResponseEntity<LogoutResponse> logout(
      HttpServletRequest request, HttpServletResponse response) {

    // Extract session ID from JWT token and mark session as logged out
    try {
      String token = jwtService.getJwtToken(request, false);
      if (token != null && !token.isBlank()) {
        var decryptedToken = encryptionService.decrypt(token);
        String sessionId = jwtService.getSessionIdFromToken(decryptedToken);

        if (sessionId != null && !sessionId.isBlank()) {
          userSessionService.logout(Long.parseLong(sessionId));
          LOG.info("Logged out session: {}", sessionId);
        } else {
          LOG.debug("No session ID found in token during logout (legacy token)");
        }
      }
    } catch (Exception e) {
      LOG.error("Error during session logout: {}", e.getMessage());
      // Continue with logout even if session tracking fails
    }

    // Clear Spring Security context and cookies
    SecurityUtils.logout(request, response);

    var responseHeaders = cookieService.addDeletedCookieToHeaders(TokenType.REFRESH);
    var logoutResponse = new LogoutResponse(OperationStatus.SUCCESS);
    SecurityUtils.clearAuthentication();

    return ResponseEntity.ok().headers(responseHeaders).body(logoutResponse);
  }

  // =========================================================================
  // UTILITY OPERATIONS
  // =========================================================================

  // Register device if Device ID header is provided
  private void registerDeviceId(Long userId, HttpServletRequest request) {
    String deviceId = request.getHeader(LoggingConstants.DEVICE_ID_HEADER);
    LOG.info("User {} logged in from IP: {}, Device ID: {}", userId, request.getRemoteAddr(), deviceId);
    if (deviceId == null || deviceId.isBlank()) {
      LOG.info("Missing Device ID header for user: {}", userId);
    } else {
      userDeviceService.createDevice(userId, deviceId, request);
    }
    return;
  }
}
