package com.kavun.config.security.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Builds the OAuth2 client registration used for web-UI login (Authorization Code + PKCE)
 * against Keycloak, driven entirely by {@link KeycloakProperties} rather than Spring Boot's
 * {@code spring.security.oauth2.client.*} auto-configuration - keeping {@code keycloak.enabled}
 * the single source of truth for whether this registration exists at all.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakClientRegistrationConfig {

  /** Registration id used throughout the web login/logout URLs (e.g. /oauth2/authorization/keycloak). */
  public static final String REGISTRATION_ID = "keycloak";

  private final KeycloakProperties keycloakProperties;

  /**
   * Discovers Keycloak's OIDC endpoints from its issuer and registers a single confidential
   * client ({@code keycloak.client-id}) for the Authorization Code flow.
   *
   * @return the client registration repository
   */
  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    String issuerUri = keycloakProperties.getServerUrl() + "/realms/" + keycloakProperties.getRealm();
    LOG.info("Discovering Keycloak OIDC configuration from issuer: {}", issuerUri);

    ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuerUri)
        .registrationId(REGISTRATION_ID)
        .clientId(keycloakProperties.getClientId())
        .clientSecret(keycloakProperties.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "profile", "email")
        .build();

    return new InMemoryClientRegistrationRepository(registration);
  }

  /**
   * Enables PKCE on the authorization request. Spring Security only adds PKCE automatically for
   * public clients; {@code kavun-api} is confidential (has a client secret), so it must be opted
   * in explicitly to get Authorization Code + PKCE as required.
   *
   * @param clientRegistrationRepository the client registration repository
   * @return the authorization request resolver with PKCE enabled
   */
  @Bean
  public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    var resolver = new DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    return resolver;
  }
}
