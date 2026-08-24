package com.kavun.web.rest.v1;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.service.security.impl.KeycloakUserService;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.constant.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keycloak authentication introspection endpoints.
 *
 * <p>The web UI authenticates via a real OIDC Authorization Code + PKCE redirect
 * (see {@code KeycloakFormLoginConfig}), and {@code /api/**} callers authenticate with a Bearer
 * JWT obtained directly from Keycloak (see {@code KeycloakSecurityConfig}) - this backend never
 * proxies passwords. These endpoints only expose read-only information for already-authenticated
 * Bearer callers and OIDC discovery metadata for clients.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_V1_AUTH_ROOT_URL)
@Tag(name = "01. Authentication", description = "Keycloak authentication introspection APIs")
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakAuthRestApi {

  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserService keycloakUserService;

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

    Map<String, Object> response = new HashMap<>();
    response.put("keycloakId", jwt.getSubject());
    response.put("username", jwt.getClaimAsString("preferred_username"));
    response.put("email", jwt.getClaimAsString("email"));
    response.put("firstName", jwt.getClaimAsString("given_name"));
    response.put("lastName", jwt.getClaimAsString("family_name"));
    response.put("emailVerified", jwt.getClaimAsBoolean("email_verified"));
    response.put("localPublicId", user != null ? user.getPublicId() : null);
    response.put("roles", jwt.getClaimAsMap("realm_access") != null
        ? jwt.getClaimAsMap("realm_access").get("roles")
        : Collections.emptyList());

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
   * Gets Keycloak OIDC configuration for API/SPA/mobile clients that authenticate directly
   * against Keycloak (Authorization Code + PKCE) rather than through this backend.
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
