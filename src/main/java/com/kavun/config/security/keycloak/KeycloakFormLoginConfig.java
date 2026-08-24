package com.kavun.config.security.keycloak;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kavun.constant.EnvConstants;
import com.kavun.constant.SecurityConstants;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * Security configuration for web (non-API) endpoints when Keycloak is enabled.
 * Uses real OAuth2 Authorization Code + PKCE login: the browser is redirected to Keycloak's own
 * hosted login page, so no password ever transits this backend.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakFormLoginConfig {

  private final Environment environment;
  private final KeycloakOidcUserService keycloakOidcUserService;

  /**
   * Configures security filter chain for web endpoints with OAuth2 Authorization Code login.
   *
   * @param http                          the HttpSecurity to configure
   * @param clientRegistrationRepository  the Keycloak client registration
   * @param authorizationRequestResolver  the PKCE-enabled authorization request resolver
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  @Order(2)
  public SecurityFilterChain keycloakWebLoginFilterChain(
      HttpSecurity http,
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizationRequestResolver authorizationRequestResolver)
      throws Exception {

    LOG.info("Configuring Keycloak OAuth2 login (Authorization Code + PKCE) for web endpoints");

    // Development mode settings (H2 console)
    if (Arrays.asList(environment.getActiveProfiles()).contains(EnvConstants.DEVELOPMENT)) {
      http.headers(headers ->
          headers
              .contentTypeOptions(withDefaults())
              .xssProtection(withDefaults())
              .cacheControl(withDefaults())
              .httpStrictTransportSecurity(withDefaults())
              .frameOptions(FrameOptionsConfig::sameOrigin));
      http.authorizeHttpRequests(req -> req.requestMatchers(PathRequest.toH2Console()).permitAll())
          .csrf(AbstractHttpConfigurer::disable)
          .cors(AbstractHttpConfigurer::disable);
    }

    // Apply to non-API endpoints
    http.securityMatcher(
            new NegatedRequestMatcher(
                new AntPathRequestMatcher(SecurityConstants.API_ROOT_URL_MAPPING)))
        .authorizeHttpRequests(requests ->
            requests
                .requestMatchers(SecurityConstants.getPublicMatchers()).permitAll()
                .anyRequest().authenticated()
        )
        // Real OIDC Authorization Code + PKCE login against Keycloak's hosted login page.
        // Single provider, so unauthenticated requests go straight to the authorization
        // endpoint instead of an intermediate "choose a provider" page.
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/oauth2/authorization/" + KeycloakClientRegistrationConfig.REGISTRATION_ID)
            .authorizationEndpoint(a -> a.authorizationRequestResolver(authorizationRequestResolver))
            .userInfoEndpoint(u -> u.oidcUserService(keycloakOidcUserService))
            .defaultSuccessUrl(SecurityConstants.ROOT_PATH, true)
            .failureUrl(SecurityConstants.LOGIN_FAILURE_URL)
        )
        // Logout also ends the Keycloak SSO session (RP-Initiated Logout), otherwise the
        // browser silently re-authenticates against the still-alive Keycloak session.
        .logout(logout ->
            logout
                .logoutRequestMatcher(new AntPathRequestMatcher(SecurityConstants.LOGOUT))
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(SecurityConstants.JSESSIONID)
                .permitAll()
        );

    return http.build();
  }

  private LogoutSuccessHandler oidcLogoutSuccessHandler(
      ClientRegistrationRepository clientRegistrationRepository) {
    var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    handler.setPostLogoutRedirectUri("{baseUrl}" + SecurityConstants.LOGIN_LOGOUT);
    return handler;
  }
}
