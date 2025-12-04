package com.kavun.config.security.keycloak;

import com.kavun.web.payload.response.KeycloakTokenResponse;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom authentication token that stores Keycloak token information.
 * Extends UsernamePasswordAuthenticationToken to include Keycloak-specific data.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
public class KeycloakAuthenticationToken extends UsernamePasswordAuthenticationToken {

  /**
   * The Keycloak token response containing access token, refresh token, etc.
   */
  private final KeycloakTokenResponse tokenResponse;

  /**
   * Creates a new KeycloakAuthenticationToken with the given credentials and authorities.
   *
   * @param principal     the principal (username)
   * @param credentials   the credentials (password)
   * @param authorities   the granted authorities (roles)
   * @param tokenResponse the Keycloak token response
   */
  public KeycloakAuthenticationToken(
      Object principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities,
      KeycloakTokenResponse tokenResponse) {
    super(principal, credentials, authorities);
    this.tokenResponse = tokenResponse;
  }

  /**
   * Gets the access token from the Keycloak response.
   *
   * @return the access token
   */
  public String getAccessToken() {
    return tokenResponse != null ? tokenResponse.getAccessToken() : null;
  }

  /**
   * Gets the refresh token from the Keycloak response.
   *
   * @return the refresh token
   */
  public String getRefreshToken() {
    return tokenResponse != null ? tokenResponse.getRefreshToken() : null;
  }
}
