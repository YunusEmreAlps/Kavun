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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * Security configuration for web (non-API) endpoints when Keycloak is enabled.
 * Uses standard form login that authenticates via Keycloak API.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakFormLoginConfig {

  private final Environment environment;
  private final KeycloakProperties keycloakProperties;

  /**
   * Configures security filter chain for web endpoints with form login.
   * Authentication is handled through Keycloak API endpoints.
   *
   * @param http the HttpSecurity to configure
   * @param mvc  the MvcRequestMatcher builder
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  @Order(2)
  public SecurityFilterChain keycloakFormLoginFilterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {

    LOG.info("Configuring Keycloak form login for web endpoints (API-based authentication)");

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
                .requestMatchers(SecurityConstants.getPublicMatchers(mvc)).permitAll()
                .anyRequest().authenticated()
        )
        // Standard form login - authentication via Keycloak REST API
        .formLogin(form ->
            form
                .loginPage(SecurityConstants.LOGIN)
                .loginProcessingUrl(SecurityConstants.LOGIN)
                .defaultSuccessUrl("/", true)
                .failureUrl(SecurityConstants.LOGIN_FAILURE_URL)
                .permitAll()
        )
        // Logout configuration with Keycloak session invalidation
        .logout(logout ->
            logout
                .logoutRequestMatcher(new AntPathRequestMatcher(SecurityConstants.LOGOUT))
                .logoutSuccessUrl(SecurityConstants.LOGIN + "?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(SecurityConstants.JSESSIONID)
                .permitAll()
        );

    return http.build();
  }
}
