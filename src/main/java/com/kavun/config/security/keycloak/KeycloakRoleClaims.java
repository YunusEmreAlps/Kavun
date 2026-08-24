package com.kavun.config.security.keycloak;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Shared extraction of Keycloak realm/client roles from a generic claims map, used by both the
 * Bearer/API path ({@link KeycloakJwtAuthenticationConverter}, claims from a {@code Jwt}) and the
 * web-login path ({@link KeycloakOidcUserService}, claims from an {@code OidcUser}/ID token) so
 * the mapping logic isn't duplicated between them.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class KeycloakRoleClaims {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  private KeycloakRoleClaims() {
  }

  /**
   * Extracts realm-level and client-level role names from the given claims map.
   *
   * @param claims   the token/userinfo claims
   * @param clientId the Keycloak client id whose resource_access roles should also be included
   * @return the raw (unprefixed) role names, e.g. {@code admin}, {@code user}
   */
  @SuppressWarnings("unchecked")
  public static Set<String> extractRoleNames(Map<String, Object> claims, String clientId) {
    if (claims == null) {
      return Collections.emptySet();
    }

    Set<String> roles = new java.util.HashSet<>();

    Object realmAccessObj = claims.get(REALM_ACCESS_CLAIM);
    if (realmAccessObj instanceof Map) {
      Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObj;
      if (realmAccess.get(ROLES_CLAIM) instanceof List) {
        roles.addAll((List<String>) realmAccess.get(ROLES_CLAIM));
      }
    }

    Object resourceAccessObj = claims.get(RESOURCE_ACCESS_CLAIM);
    if (resourceAccessObj instanceof Map && clientId != null) {
      Map<String, Object> resourceAccess = (Map<String, Object>) resourceAccessObj;
      Object clientAccessObj = resourceAccess.get(clientId);
      if (clientAccessObj instanceof Map) {
        Map<String, Object> clientAccess = (Map<String, Object>) clientAccessObj;
        if (clientAccess.get(ROLES_CLAIM) instanceof List) {
          roles.addAll((List<String>) clientAccess.get(ROLES_CLAIM));
        }
      }
    }

    return roles;
  }

  /**
   * Extracts realm/client roles from the claims and maps them to normalized {@code ROLE_*}
   * granted authorities.
   *
   * @param claims   the token/userinfo claims
   * @param clientId the Keycloak client id whose resource_access roles should also be included
   * @return the mapped granted authorities
   */
  public static Collection<GrantedAuthority> extractAuthorities(
      Map<String, Object> claims, String clientId) {
    return extractRoleNames(claims, clientId).stream()
        .map(KeycloakRoleClaims::toGrantedAuthority)
        .collect(Collectors.toSet());
  }

  /**
   * Normalizes a raw Keycloak role name (e.g. {@code admin}) into a Spring Security authority
   * (e.g. {@code ROLE_ADMIN}).
   *
   * @param role the raw role name
   * @return the granted authority
   */
  public static GrantedAuthority toGrantedAuthority(String role) {
    String normalized = role.toUpperCase();
    if (!normalized.startsWith(ROLE_PREFIX)) {
      normalized = ROLE_PREFIX + normalized;
    }
    return new SimpleGrantedAuthority(normalized);
  }

  /**
   * Concatenates two authority collections into a single set. Convenience for callers that need
   * to merge Keycloak-derived roles with another authority source (e.g. default OAuth2 scopes).
   *
   * @param first  the first collection
   * @param second the second collection
   * @return the merged set
   */
  public static Collection<GrantedAuthority> merge(
      Collection<GrantedAuthority> first, Collection<GrantedAuthority> second) {
    return Stream.concat(first.stream(), second.stream()).collect(Collectors.toSet());
  }
}
