package com.kavun.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Adds the platform's configuration, {@code kavun-defaults.properties} and {@code
 * kavun-defaults-{profile}.properties}, below the application's own. An application built on the
 * platform inherits them and overrides one by setting the property in its {@code
 * application.properties} or {@code application-{profile}.properties}.
 *
 * <p>Registered in {@code META-INF/spring.factories}; it runs after the active profiles are known
 * and before logging is initialised, so the defaults may contain {@code logging.*} properties.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public class KavunDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String NAME = "kavun-defaults";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    List<String> profiles = new ArrayList<>(List.of(environment.getActiveProfiles()));
    // Like application-{profile}.properties, a later profile wins over an earlier one.
    Collections.reverse(profiles);
    for (String profile : profiles) {
      addLast(environment, NAME + "-" + profile + ".properties");
    }
    addLast(environment, NAME + ".properties");
    DefaultPropertiesPropertySource.moveToEnd(environment);
  }

  private static void addLast(ConfigurableEnvironment environment, String location) {
    var resource = new ClassPathResource(location);
    if (!resource.exists()) {
      return;
    }
    try {
      new PropertiesPropertySourceLoader()
          .load(location, resource)
          .forEach(environment.getPropertySources()::addLast);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + location, e);
    }
  }
}
