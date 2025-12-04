package com.kavun.config.security.keycloak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Converts Keycloak JWT tokens to Spring Security authentication tokens.
 * Extracts roles from Keycloak's realm_access and resource_access claims.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
  private final KeycloakProperties keycloakProperties;

  public KeycloakJwtAuthenticationConverter(KeycloakProperties keycloakProperties) {
    this.keycloakProperties = keycloakProperties;
    this.jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
  }

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    Collection<GrantedAuthority> authorities = Stream.concat(
        jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
        extractKeycloakRoles(jwt).stream()
    ).collect(Collectors.toSet());

    String principalClaimName = jwt.getClaimAsString("preferred_username");
    if (principalClaimName == null) {
      principalClaimName = jwt.getSubject();
    }

    LOG.debug("Converted JWT for user: {} with authorities: {}", principalClaimName, authorities);

    return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
  }

  /**
   * Extracts roles from Keycloak JWT token.
   *
   * @param jwt the JWT token
   * @return collection of granted authorities
   */
  private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
    // Extract realm roles
    Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

    // Extract client roles
    Collection<GrantedAuthority> clientRoles = extractClientRoles(jwt);

    return Stream.concat(realmRoles.stream(), clientRoles.stream())
        .collect(Collectors.toSet());
  }

  /**
   * Extracts realm-level roles from the JWT.
   *
   * @param jwt the JWT token
   * @return collection of realm role authorities
   */
  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
    if (realmAccess == null) {
      return Collections.emptyList();
    }

    Object rolesObj = realmAccess.get(ROLES_CLAIM);
    if (!(rolesObj instanceof List)) {
      return Collections.emptyList();
    }

    List<String> roles = (List<String>) rolesObj;
    return roles.stream()
        .map(this::mapToGrantedAuthority)
        .collect(Collectors.toList());
  }

  /**
   * Extracts client-level roles from the JWT.
   *
   * @param jwt the JWT token
   * @return collection of client role authorities
   */
  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
    if (resourceAccess == null) {
      return Collections.emptyList();
    }

    String clientId = keycloakProperties.getClientId();
    Object clientAccessObj = resourceAccess.get(clientId);
    if (!(clientAccessObj instanceof Map)) {
      return Collections.emptyList();
    }

    Map<String, Object> clientAccess = (Map<String, Object>) clientAccessObj;
    Object rolesObj = clientAccess.get(ROLES_CLAIM);
    if (!(rolesObj instanceof List)) {
      return Collections.emptyList();
    }

    List<String> roles = (List<String>) rolesObj;
    return roles.stream()
        .map(this::mapToGrantedAuthority)
        .collect(Collectors.toList());
  }

  /**
   * Maps a role name to a GrantedAuthority.
   * Adds ROLE_ prefix if not already present and converts to uppercase.
   *
   * @param role the role name
   * @return the granted authority
   */
  private GrantedAuthority mapToGrantedAuthority(String role) {
    String normalizedRole = role.toUpperCase();
    if (!normalizedRole.startsWith(ROLE_PREFIX)) {
      normalizedRole = ROLE_PREFIX + normalizedRole;
    }
    return new SimpleGrantedAuthority(normalizedRole);
  }
}
