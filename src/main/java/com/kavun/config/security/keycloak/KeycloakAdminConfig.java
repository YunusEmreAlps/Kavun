package com.kavun.config.security.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client configuration.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakAdminConfig {

  private final KeycloakProperties keycloakProperties;

  /**
   * Creates a Keycloak admin client for administrative operations.
   *
   * @return Keycloak admin client instance
   */
  @Bean
  public Keycloak keycloakAdmin() {
    LOG.info("Initializing Keycloak Admin Client for server: {}", keycloakProperties.getServerUrl());

    return KeycloakBuilder.builder()
        .serverUrl(keycloakProperties.getServerUrl())
        .realm("master")
        .clientId("admin-cli")
        .username(keycloakProperties.getAdminUsername())
        .password(keycloakProperties.getAdminPassword())
        .grantType(OAuth2Constants.PASSWORD)
        .build();
  }
}
