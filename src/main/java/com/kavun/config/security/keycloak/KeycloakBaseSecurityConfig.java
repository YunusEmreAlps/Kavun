package com.kavun.config.security.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base security configuration for Keycloak-enabled mode.
 * Provides common security beans and enables method-level security.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakBaseSecurityConfig {

  /**
   * Password encoder bean for password hashing operations.
   * Used for local password operations if needed.
   *
   * @return BCrypt password encoder
   */
  @Bean
  public PasswordEncoder keycloakPasswordEncoder() {
    LOG.info("Initializing BCrypt password encoder for Keycloak mode");
    return new BCryptPasswordEncoder(12);
  }
}
