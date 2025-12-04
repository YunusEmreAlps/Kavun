package com.kavun.web.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object for Keycloak token endpoint.
 * Contains access token, refresh token, and related metadata.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakTokenResponse {

  /**
   * The access token (JWT) for API authorization.
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * The refresh token for obtaining new access tokens.
   */
  @JsonProperty("refresh_token")
  private String refreshToken;

  /**
   * The ID token containing user identity claims.
   */
  @JsonProperty("id_token")
  private String idToken;

  /**
   * Token type (usually "Bearer").
   */
  @JsonProperty("token_type")
  private String tokenType;

  /**
   * Access token expiration time in seconds.
   */
  @JsonProperty("expires_in")
  private Long expiresIn;

  /**
   * Refresh token expiration time in seconds.
   */
  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  /**
   * Time before which the token must not be accepted (in seconds).
   */
  @JsonProperty("not-before-policy")
  private Long notBeforePolicy;

  /**
   * Session state identifier.
   */
  @JsonProperty("session_state")
  private String sessionState;

  /**
   * The scope of the access token.
   */
  private String scope;
}
