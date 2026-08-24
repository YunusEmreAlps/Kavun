package com.kavun.config.security.keycloak;

import com.kavun.backend.service.security.impl.KeycloakUserService;
import java.util.Collection;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Loads the OIDC user for web-UI login, merges Keycloak realm/client roles into the principal's
 * authorities (mirroring what {@link KeycloakJwtAuthenticationConverter} does for the Bearer/API
 * path), and just-in-time provisions/updates the corresponding local {@code User} row so the
 * existing dynamic page/action permission system keeps working for Keycloak-authenticated users.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakOidcUserService extends OidcUserService {

  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserService keycloakUserService;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    Collection<GrantedAuthority> authorities = KeycloakRoleClaims.merge(
        new HashSet<>(oidcUser.getAuthorities()),
        KeycloakRoleClaims.extractAuthorities(oidcUser.getClaims(), keycloakProperties.getClientId()));

    keycloakUserService.syncUserFromClaims(oidcUser.getClaims());

    LOG.debug("Loaded OIDC user: {} with authorities: {}", oidcUser.getName(), authorities);

    return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
  }
}
