package com.kavun.backend.service.security;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.security.impl.KeycloakUserService;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated user as a {@link UserDto}, regardless of which identity
 * provider produced the {@code Authentication} principal.
 *
 * <p>This is the bridge between authentication (local {@code UserDetailsBuilder}, Keycloak Bearer
 * {@code Jwt}, or Keycloak web-login {@code OidcUser}) and the existing DB-driven page/action
 * permission system ({@code PermissionAspect}/{@code PermissionCheckService}), which only knows
 * about local {@code User} rows. Always registered - Keycloak support is used only when
 * {@code keycloak.enabled=true} and {@link KeycloakUserService} is actually available.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

  private final ObjectProvider<KeycloakUserService> keycloakUserServiceProvider;

  /**
   * Resolves the currently authenticated user, JIT-provisioning a local {@code User} row for
   * Keycloak principals if one doesn't already exist.
   *
   * @return the current user, or null if unauthenticated or the principal type is unrecognized
   */
  public UserDto resolve() {
    Object principal = SecurityUtils.isAuthenticated() ? SecurityUtils.getAuthentication().getPrincipal() : null;

    if (principal instanceof UserDetailsBuilder userDetailsBuilder) {
      return UserUtils.convertToUserDto(userDetailsBuilder);
    }
    if (principal instanceof Jwt jwt) {
      return resolveViaKeycloak(jwt.getClaims());
    }
    if (principal instanceof OidcUser oidcUser) {
      return resolveViaKeycloak(oidcUser.getClaims());
    }
    return null;
  }

  private UserDto resolveViaKeycloak(java.util.Map<String, Object> claims) {
    KeycloakUserService keycloakUserService = keycloakUserServiceProvider.getIfAvailable();
    return keycloakUserService == null ? null : keycloakUserService.resolveOrProvisionUserDto(claims);
  }
}
