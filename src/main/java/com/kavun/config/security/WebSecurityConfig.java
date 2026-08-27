package com.kavun.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.kavun.constant.EnvConstants;
import com.kavun.constant.SecurityConstants;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This configuration handles the small set of non-API routes served without authentication (home,
 * login redirect, error pages, actuator/swagger, static assets). There is no HTML login form or
 * session-authenticated page in the application anymore, so every other non-API request is
 * denied. This configuration is considered before ApiWebSecurityConfigurationAdapter since it has
 * an @Order value after 1 (no @Order defaults to last).
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

  private final Environment environment;

  /**
   * Configure the {@link HttpSecurity}. Typically, subclasses should not call supper as it may
   * override their configuration.
   *
   * @param http the {@link HttpSecurity} to modify.
   * @throws Exception thrown when error happens during authentication.
   */
  @Bean
  public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {

    // if we are running with dev profile, disable csrf and frame options to enable h2 to work.
    if (Arrays.asList(environment.getActiveProfiles()).contains(EnvConstants.DEVELOPMENT)) {
      http.headers(
          (headers) ->
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

    http.securityMatcher(request -> !request.getRequestURI().startsWith("/api"))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(SecurityConstants.getPublicMatchers())
                    .permitAll()
                    .anyRequest()
                    .denyAll());

    return http.build();
  }
}
