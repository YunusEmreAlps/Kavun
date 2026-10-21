package com.kavun.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kavun.annotation.LoggingFilter;
import com.kavun.config.security.jwt.JwtAuthTokenFilter;
import com.kavun.config.security.jwt.JwtAuthenticationEntryPoint;
import com.kavun.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This configuration handles api web requests with stateless session.
 * DEPRECATED: This configuration is disabled when Keycloak is enabled.
 * Use KeycloakSecurityConfig instead for OAuth2 Resource Server configuration.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 * @deprecated Use {@link KeycloakSecurityConfig} when Keycloak is enabled
 */
@Deprecated
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "false", matchIfMissing = true)
public class ApiWebSecurityConfig {

  private final JwtAuthTokenFilter jwtAuthTokenFilter;
  private final LoggingFilter loggingFilter;
  private final JwtAuthenticationEntryPoint unauthorizedHandler;
  private final UserDetailsService userDetailsService;

  /**
   * Configure the {@link HttpSecurity}. Typically, subclasses should not call supper as it may
   * override their configuration.
   *
   * @param http the {@link HttpSecurity} to modify.
   * @throws Exception thrown when error happens during authentication.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

    // Match any incoming request focusing on the /api/** to use this security filter chain
    http.securityMatcher(SecurityConstants.API_ROOT_URL_MAPPING);

    // Use a custom exception handler when authentication fails
    http.exceptionHandling(
            (exceptionHandling) -> exceptionHandling.authenticationEntryPoint(unauthorizedHandler))

        // guarantee that the application won't create any session at all by using stateless policy
        .sessionManagement(
            (sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // The filter chain only authenticates. Every API endpoint declares its own access through
        // @PublicEndpoint, @AuthenticatedEndpoint or @RequirePermission, enforced per request by
        // EndpointAuthorizationInterceptor; there is no path-based allow list.
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .cors(withDefaults())

        // If your application is attaching the credentials via an Authorization header,
        // then the browser can't automatically authenticate the requests,
        // and CSRF isn't possible.
        .csrf(AbstractHttpConfigurer::disable)
        .userDetailsService(userDetailsService)
        .addFilterBefore(jwtAuthTokenFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(loggingFilter, ExceptionTranslationFilter.class);

    return http.build();
  }
}
