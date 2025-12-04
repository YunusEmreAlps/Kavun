package com.kavun.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kavun.config.security.keycloak.KeycloakJwtAuthenticationConverter;
import com.kavun.config.security.keycloak.KeycloakProperties;
import com.kavun.constant.AdminConstants;
import com.kavun.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration for API endpoints using Keycloak OAuth2 Resource Server.
 * This configuration handles JWT token validation and authorization for REST API endpoints.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakSecurityConfig {

  private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
  private final KeycloakProperties keycloakProperties;

  /**
   * Configures the security filter chain for API endpoints with Keycloak OAuth2 Resource Server.
   *
   * @param http the HttpSecurity to configure
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  @Order(1)
  public SecurityFilterChain keycloakApiFilterChain(HttpSecurity http) throws Exception {
    LOG.info("Configuring Keycloak OAuth2 Resource Server for API endpoints");

    // Match any incoming request focusing on the /api/** to use this security filter chain
    http.securityMatcher(SecurityConstants.API_ROOT_URL_MAPPING);

    http
        // Configure as OAuth2 Resource Server with JWT
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter)
            )
        )
        // Stateless session management
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        // Authorization rules
        .authorizeHttpRequests(requests -> {
          // Public endpoints
          requests
              .requestMatchers(SecurityConstants.ERROR_URL_MAPPING).permitAll()
              // Allow token endpoint for getting tokens
              .requestMatchers(new AntPathRequestMatcher(SecurityConstants.API_V1_AUTH_URL_MAPPING)).permitAll()
              // Allow user registration
              .requestMatchers(new AntPathRequestMatcher(AdminConstants.API_V1_USERS_ROOT_URL, HttpMethod.POST.name())).permitAll()
              // Swagger/OpenAPI endpoints
              .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
              // Health check endpoints
              .requestMatchers("/actuator/health/**", "/actuator/prometheus").permitAll();

          // All other API requests require authentication
          requests.anyRequest().authenticated();
        })
        // Enable CORS
        .cors(withDefaults())
        // Disable CSRF for stateless API
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
