package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.security.CookieService;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.enums.TokenType;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.request.LoginRequest;
import com.kavun.web.payload.response.JwtResponseBuilder;
import com.kavun.web.payload.response.LogoutResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Date;
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
 * This class attempt to authenticate with AuthenticationManager bean, add an authentication object
 * to SecurityContextHolder then Generate JWT token, then return JWT to a client.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_V1_AUTH_ROOT_URL)
public class AuthRestApi {

  @Value("${access-token-expiration-in-minutes}")
  private int accessTokenExpirationInMinutes;

  private final JwtService jwtService;
  private final CookieService cookieService;
  private final EncryptionService encryptionService;
  private final UserDetailsService userDetailsService;
  private final DaoAuthenticationProvider authenticationManager;

  /**
   * Attempts to authenticate with the provided credentials. If successful, a JWT token is returned
   * with some user details.
   *
   * <p>A refresh token is generated and returned as a cookie.
   *
   * @param refreshToken The refresh token
   * @param loginRequest the login request
   * @return the jwt token details
   */
  @SecurityRequirements
  @Loggable(level = "debug")
  @PostMapping(value = SecurityConstants.LOGIN)
  public ResponseEntity<JwtResponseBuilder> authenticateUser(
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

    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(JwtResponseBuilder.buildJwtResponse(encryptedAccessToken));
  }

  /**
   * Refreshes the current access token and refresh token accordingly.
   *
   * @param refreshToken The refresh token
   * @param request The request
   * @return the jwt token details
   */
  @SecurityRequirements
  @Loggable(level = "error")
  @GetMapping(value = SecurityConstants.REFRESH_TOKEN)
  public ResponseEntity<JwtResponseBuilder> refreshToken(
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

    return ResponseEntity.ok(JwtResponseBuilder.buildJwtResponse(encryptedAccessToken));
  }

  /**
   * Logout the user from the system and clear all cookies from request and response.
   *
   * @param request the request
   * @param response the response
   * @return response entity
   */
  @SecurityRequirements
  @Loggable(level = "warn")
  @DeleteMapping(value = SecurityConstants.LOGOUT)
  public ResponseEntity<LogoutResponse> logout(
      HttpServletRequest request, HttpServletResponse response) {
    SecurityUtils.logout(request, response);

    var responseHeaders = cookieService.addDeletedCookieToHeaders(TokenType.REFRESH);
    var logoutResponse = new LogoutResponse(OperationStatus.SUCCESS);
    SecurityUtils.clearAuthentication();

    return ResponseEntity.ok().headers(responseHeaders).body(logoutResponse);
  }

  /**
   * Creates a refresh token if expired and adds it to the cookies.
   *
   * @param username the username
   * @param isRefreshValid if the refresh token is valid
   * @param headers the http headers
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
