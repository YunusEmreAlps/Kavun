package com.kavun.config.security.keycloak;

import com.kavun.backend.service.security.impl.KeycloakUserService;
import com.kavun.web.payload.response.KeycloakTokenResponse;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * AuthenticationProvider that authenticates users against Keycloak.
 * This provider is used for form login and validates credentials via Keycloak's token endpoint.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakAuthenticationProvider implements AuthenticationProvider {

  private final KeycloakProperties keycloakProperties;
  private final KeycloakUserService keycloakUserService;
  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    LOG.debug("Attempting to authenticate user '{}' via Keycloak", username);

    try {
      // Authenticate via Keycloak token endpoint
      KeycloakTokenResponse tokenResponse = authenticateWithKeycloak(username, password);

      if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
        throw new BadCredentialsException("Invalid credentials");
      }

      LOG.debug("User '{}' successfully authenticated via Keycloak", username);

      // Sync user to local database
      keycloakUserService.syncUserFromToken(tokenResponse.getAccessToken());

      // Extract roles from token and create authorities
      Set<String> roles = keycloakUserService.extractRolesFromToken(tokenResponse.getAccessToken());
      List<SimpleGrantedAuthority> authorities = roles.stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
          .collect(Collectors.toList());

      LOG.debug("User '{}' has roles: {}", username, roles);

      // Create authenticated token with user details and token
      KeycloakAuthenticationToken authToken = new KeycloakAuthenticationToken(
          username,
          password,
          authorities,
          tokenResponse
      );

      return authToken;

    } catch (RestClientException e) {
      LOG.error("Keycloak authentication failed for user '{}': {}", username, e.getMessage());
      throw new BadCredentialsException("Authentication failed: " + e.getMessage(), e);
    }
  }

  /**
   * Authenticate user with Keycloak using Resource Owner Password Credentials grant.
   *
   * @param username the username
   * @param password the password
   * @return the token response from Keycloak
   */
  private KeycloakTokenResponse authenticateWithKeycloak(String username, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", keycloakProperties.getClientId());
    body.add("client_secret", keycloakProperties.getClientSecret());
    body.add("username", username);
    body.add("password", password);
    body.add("scope", "openid profile email");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
          keycloakProperties.getTokenUri(),
          request,
          KeycloakTokenResponse.class
      );

      return response.getBody();
    } catch (RestClientException e) {
      LOG.error("Failed to authenticate with Keycloak: {}", e.getMessage());
      throw new BadCredentialsException("Invalid username or password");
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
