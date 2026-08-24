package com.kavun.config.security.keycloak;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Converts Keycloak JWT tokens to Spring Security authentication tokens.
 * Extracts roles from Keycloak's realm_access and resource_access claims via
 * {@link KeycloakRoleClaims}.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
  private final KeycloakProperties keycloakProperties;

  public KeycloakJwtAuthenticationConverter(KeycloakProperties keycloakProperties) {
    this.keycloakProperties = keycloakProperties;
    this.jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  }

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    Collection<GrantedAuthority> authorities = KeycloakRoleClaims.merge(
        jwtGrantedAuthoritiesConverter.convert(jwt),
        KeycloakRoleClaims.extractAuthorities(jwt.getClaims(), keycloakProperties.getClientId()));

    String principalClaimName = jwt.getClaimAsString("preferred_username");
    if (principalClaimName == null) {
      principalClaimName = jwt.getSubject();
    }

    LOG.debug("Converted JWT for user: {} with authorities: {}", principalClaimName, authorities);

    return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
  }
}
