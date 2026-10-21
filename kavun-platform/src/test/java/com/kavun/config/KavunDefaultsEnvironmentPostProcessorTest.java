package com.kavun.config;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class KavunDefaultsEnvironmentPostProcessorTest {

  private static final String ADMIN_BYPASS = "security.permission.admin-bypass-enabled";

  @Test
  void addsThePlatformDefaults() {
    var environment = postProcess(new StandardEnvironment());

    Assertions.assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
    Assertions.assertEquals("false", environment.getProperty(ADMIN_BYPASS));
  }

  @Test
  void profileDefaultsApplyOnTopOfTheDefaults() {
    var environment = new StandardEnvironment();
    environment.setActiveProfiles("development");

    Assertions.assertEquals("true", postProcess(environment).getProperty(ADMIN_BYPASS));
  }

  @Test
  void laterProfileWins() {
    var environment = new StandardEnvironment();
    environment.setActiveProfiles("development", "production");

    Assertions.assertEquals("false", postProcess(environment).getProperty(ADMIN_BYPASS));
  }

  @Test
  void applicationPropertiesOverrideTheDefaults() {
    var environment = new StandardEnvironment();
    environment.setActiveProfiles("development");
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("application", Map.of(ADMIN_BYPASS, "false")));

    Assertions.assertEquals("false", postProcess(environment).getProperty(ADMIN_BYPASS));
  }

  private static StandardEnvironment postProcess(StandardEnvironment environment) {
    new KavunDefaultsEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    return environment;
  }
}
