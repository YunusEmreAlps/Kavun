package com.kavun.config.security.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak configuration properties.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

  /**
   * Enable or disable Keycloak integration.
   */
  private boolean enabled = true;

  /**
   * Keycloak server URL.
   */
  private String serverUrl;

  /**
   * Keycloak realm name.
   */
  private String realm;

  /**
   * Keycloak client ID.
   */
  private String clientId;

  /**
   * Keycloak client secret.
   */
  private String clientSecret;

  /**
   * Keycloak admin username for admin operations.
   */
  private String adminUsername;

  /**
   * Keycloak admin password for admin operations.
   */
  private String adminPassword;

  /**
   * Keycloak token endpoint URI.
   */
  private String tokenUri;

  /**
   * Get the token endpoint URI, building from serverUrl and realm if not explicitly set.
   *
   * @return The token endpoint URI
   */
  public String getTokenUri() {
    if (tokenUri != null && !tokenUri.isBlank()) {
      return tokenUri;
    }
    return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
  }
}
