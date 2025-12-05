package com.kavun.web.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.SecurityConstants;
import com.kavun.shared.util.core.SecurityUtils;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * OAuth2/OpenID Connect compliant authentication response.
 * Following RFC 6749 and best practices.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * The access token string (JWT).
   * OAuth2 standard field name.
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * The type of token (always "Bearer").
   * OAuth2 standard field name.
   */
  @JsonProperty("token_type")
  @Builder.Default
  private String tokenType = SecurityConstants.BEARER;

  /**
   * Token expiration time in seconds.
   * OAuth2 standard field name.
   */
  @JsonProperty("expires_in")
  private Long expiresIn;

  /**
   * Token for refreshing the access token.
   * OAuth2 standard field name.
   */
  @JsonProperty("refresh_token")
  private String refreshToken;

  /**
   * Refresh token expiration time in seconds.
   */
  @JsonProperty("refresh_expires_in")
  private Long refreshExpiresIn;

  /**
   * Unix timestamp when token was issued.
   */
  @JsonProperty("issued_at")
  private Long issuedAt;

  /**
   * User information (optional, for convenience).
   * Can be fetched separately via /userinfo endpoint.
   */
  private UserInfo user;

  /**
   * User information nested object.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class UserInfo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /**
     * User's unique public identifier (UUID).
     */
    private String id;

    /**
     * Username.
     */
    private String username;

    /**
     * Email address.
     */
    private String email;

    /**
     * First name.
     */
    @JsonProperty("first_name")
    private String firstName;

    /**
     * Last name.
     */
    @JsonProperty("last_name")
    private String lastName;

    /**
     * User's roles/authorities.
     */
    private List<String> roles;

    /**
     * Whether the user's email is verified.
     */
    @JsonProperty("email_verified")
    private Boolean emailVerified;

    /**
     * Whether the account is enabled.
     */
    private Boolean enabled;
  }

  /**
   * Build AuthResponse from JWT token and user details.
   *
   * @param accessToken the JWT access token
   * @param expiresInSeconds token expiration in seconds
   * @return AuthResponse
   */
  public static AuthResponse of(String accessToken, long expiresInSeconds) {
    return of(accessToken, expiresInSeconds, null, null);
  }

  /**
   * Build AuthResponse from JWT token, expiration, and user details.
   *
   * @param accessToken the JWT access token
   * @param expiresInSeconds token expiration in seconds
   * @param refreshToken the refresh token (optional)
   * @param userDetails the user details (optional)
   * @return AuthResponse
   */
  public static AuthResponse of(
      String accessToken,
      long expiresInSeconds,
      String refreshToken,
      UserDetailsBuilder userDetails) {

    var localUserDetails = userDetails;
    if (Objects.isNull(localUserDetails)) {
      localUserDetails = SecurityUtils.getAuthenticatedUserDetails();
    }

    AuthResponseBuilder builder =
        AuthResponse.builder()
            .accessToken(accessToken)
            .tokenType(SecurityConstants.BEARER)
            .expiresIn(expiresInSeconds)
            .refreshToken(refreshToken)
            .issuedAt(Instant.now().getEpochSecond());

    if (Objects.nonNull(localUserDetails)) {
      List<String> roleList = new ArrayList<>();
      for (GrantedAuthority authority : localUserDetails.getAuthorities()) {
        roleList.add(authority.getAuthority());
      }

      UserInfo userInfo =
          UserInfo.builder()
              .id(localUserDetails.getPublicId())
              .username(localUserDetails.getUsername())
              .email(localUserDetails.getEmail())
              .roles(roleList)
              .enabled(localUserDetails.isEnabled())
              .emailVerified(localUserDetails.isEnabled()) // Assuming enabled = verified
              .build();

      builder.user(userInfo);
    }

    return builder.build();
  }

  /**
   * Build a minimal AuthResponse (just token info, no user).
   *
   * @param accessToken the JWT access token
   * @param expiresInSeconds token expiration in seconds
   * @return AuthResponse without user info
   */
  public static AuthResponse minimal(String accessToken, long expiresInSeconds) {
    return AuthResponse.builder()
        .accessToken(accessToken)
        .tokenType(SecurityConstants.BEARER)
        .expiresIn(expiresInSeconds)
        .issuedAt(Instant.now().getEpochSecond())
        .build();
  }
}
