package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.service.security.impl.KeycloakUserService;
import com.kavun.backend.service.user.UserService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.OperationStatus;
import com.kavun.web.payload.request.LoginRequest;
import com.kavun.web.payload.response.KeycloakTokenResponse;
import com.kavun.web.payload.response.LogoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Keycloak Authentication REST API controller.
 * Handles user authentication through Keycloak OAuth2/OpenID Connect.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_V1_AUTH_ROOT_URL)
@Tag(name = "01. Authentication", description = "Keycloak-based authentication APIs")
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakAuthRestApi {

  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserService keycloakUserService;
  private final UserService userService;
  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Authenticates user with username and password through Keycloak.
   * Returns access token, refresh token, and token metadata.
   *
   * @param loginRequest the login credentials
   * @return the token response from Keycloak
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(value = SecurityConstants.LOGIN)
  @Operation(summary = "Login with Keycloak", description = "Authenticate user and get access/refresh tokens from Keycloak")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
    LOG.info("Login attempt for user: {}", loginRequest.getUsername());

    try {
      String tokenUrl = buildTokenUrl();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "password");
      body.add("client_id", keycloakProperties.getClientId());
      body.add("client_secret", keycloakProperties.getClientSecret());
      body.add("username", loginRequest.getUsername());
      body.add("password", loginRequest.getPassword());
      body.add("scope", "openid profile email");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
          tokenUrl,
          request,
          KeycloakTokenResponse.class
      );

      LOG.info("Login successful for user: {}", loginRequest.getUsername());
      return ResponseEntity.ok(response.getBody());

    } catch (HttpClientErrorException.Unauthorized e) {
      LOG.warn("Invalid credentials for user: {}", loginRequest.getUsername());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid credentials", "message", "Username or password is incorrect"));

    } catch (HttpClientErrorException e) {
      LOG.error("Keycloak authentication error: {}", e.getMessage());
      return ResponseEntity.status(e.getStatusCode())
          .body(Map.of("error", "Authentication failed", "message", e.getResponseBodyAsString()));

    } catch (Exception e) {
      LOG.error("Login error for user: {}", loginRequest.getUsername(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Authentication service unavailable", "message", "Please try again later"));
    }
  }

  /**
   * Refreshes the access token using a refresh token.
   *
   * @param refreshToken the refresh token
   * @return new access token response
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(value = SecurityConstants.REFRESH_TOKEN)
  @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
  public ResponseEntity<?> refreshToken(@RequestParam("refresh_token") String refreshToken) {
    LOG.debug("Refreshing access token");

    try {
      String tokenUrl = buildTokenUrl();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", "refresh_token");
      body.add("client_id", keycloakProperties.getClientId());
      body.add("client_secret", keycloakProperties.getClientSecret());
      body.add("refresh_token", refreshToken);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

      ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
          tokenUrl,
          request,
          KeycloakTokenResponse.class
      );

      LOG.debug("Token refresh successful");
      return ResponseEntity.ok(response.getBody());

    } catch (HttpClientErrorException.BadRequest e) {
      LOG.warn("Invalid or expired refresh token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid refresh token", "message", "Refresh token is invalid or expired"));

    } catch (Exception e) {
      LOG.error("Token refresh error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Token refresh failed", "message", "Please try again later"));
    }
  }

  /**
   * Logs out the user by invalidating the refresh token in Keycloak.
   *
   * @param refreshToken the refresh token to invalidate
   * @param request      the HTTP request
   * @param response     the HTTP response
   * @return logout response
   */
  @Loggable
  @DeleteMapping(value = SecurityConstants.LOGOUT)
  @Operation(summary = "Logout", description = "Invalidate refresh token and logout from Keycloak")
  public ResponseEntity<LogoutResponse> logout(
      @RequestParam(value = "refresh_token", required = false) String refreshToken,
      HttpServletRequest request,
      HttpServletResponse response) {

    LOG.info("Logout request received");

    try {
      if (refreshToken != null && !refreshToken.isEmpty()) {
        String logoutUrl = buildLogoutUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", keycloakProperties.getClientId());
        body.add("client_secret", keycloakProperties.getClientSecret());
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> logoutRequest = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(logoutUrl, logoutRequest, Void.class);
        LOG.info("Keycloak session invalidated successfully");
      }

      return ResponseEntity.ok(new LogoutResponse(OperationStatus.SUCCESS));

    } catch (Exception e) {
      LOG.error("Logout error", e);
      // Still return success as client-side cleanup should proceed
      return ResponseEntity.ok(new LogoutResponse(OperationStatus.SUCCESS));
    }
  }

  /**
   * Gets the current authenticated user's information.
   * Also syncs user to local database if not exists.
   *
   * @param jwt the JWT token from Keycloak
   * @return user information
   */
  @Loggable
  @GetMapping(value = "/me")
  @Operation(summary = "Get Current User", description = "Get information about the currently authenticated user")
  public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Not authenticated"));
    }

    // Sync user from Keycloak to local database
    User user = keycloakUserService.syncUserFromKeycloak(jwt);

    // Build response map with null-safe values
    java.util.Map<String, Object> response = new java.util.HashMap<>();
    response.put("keycloakId", jwt.getSubject());
    response.put("username", jwt.getClaimAsString("preferred_username"));
    response.put("email", jwt.getClaimAsString("email"));
    response.put("firstName", jwt.getClaimAsString("given_name"));
    response.put("lastName", jwt.getClaimAsString("family_name"));
    response.put("emailVerified", jwt.getClaimAsBoolean("email_verified"));
    response.put("localPublicId", user != null ? user.getPublicId() : null);
    response.put("roles", jwt.getClaimAsMap("realm_access") != null
        ? jwt.getClaimAsMap("realm_access").get("roles")
        : java.util.Collections.emptyList());

    return ResponseEntity.ok(response);
  }

  /**
   * Validates the current access token.
   *
   * @param jwt the JWT token
   * @return validation status
   */
  @Loggable
  @GetMapping(value = "/validate")
  @Operation(summary = "Validate Token", description = "Check if the current access token is valid")
  public ResponseEntity<?> validateToken(@AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("valid", false, "message", "Token is invalid or expired"));
    }

    return ResponseEntity.ok(Map.of(
        "valid", true,
        "expiresAt", jwt.getExpiresAt(),
        "issuedAt", jwt.getIssuedAt(),
        "subject", jwt.getSubject()
    ));
  }

  /**
   * Gets Keycloak configuration for frontend clients.
   *
   * @return Keycloak configuration
   */
  @Loggable
  @SecurityRequirements
  @GetMapping(value = "/config")
  @Operation(summary = "Get Auth Config", description = "Get Keycloak configuration for frontend clients")
  public ResponseEntity<?> getAuthConfig() {
    return ResponseEntity.ok(Map.of(
        "authServerUrl", keycloakProperties.getServerUrl(),
        "realm", keycloakProperties.getRealm(),
        "clientId", keycloakProperties.getClientId(),
        "tokenEndpoint", buildTokenUrl(),
        "authEndpoint", buildAuthorizationUrl(),
        "logoutEndpoint", buildLogoutUrl(),
        "userInfoEndpoint", buildUserInfoUrl()
    ));
  }

  // =========================================================================
  // UTILITY METHODS
  // =========================================================================

  private String buildTokenUrl() {
    return String.format("%s/realms/%s/protocol/openid-connect/token",
        keycloakProperties.getServerUrl(),
        keycloakProperties.getRealm());
  }

  private String buildLogoutUrl() {
    return String.format("%s/realms/%s/protocol/openid-connect/logout",
        keycloakProperties.getServerUrl(),
        keycloakProperties.getRealm());
  }

  private String buildAuthorizationUrl() {
    return String.format("%s/realms/%s/protocol/openid-connect/auth",
        keycloakProperties.getServerUrl(),
        keycloakProperties.getRealm());
  }

  private String buildUserInfoUrl() {
    return String.format("%s/realms/%s/protocol/openid-connect/userinfo",
        keycloakProperties.getServerUrl(),
        keycloakProperties.getRealm());
  }
}
