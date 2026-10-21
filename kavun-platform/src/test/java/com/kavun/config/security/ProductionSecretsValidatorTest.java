package com.kavun.config.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductionSecretsValidatorTest {

  private static final String DEFAULT_JWT_SECRET =
      "u7x!A%D*G-KaPdSgVkYp2s5v8y/B?E(H+MbQeThWmZq4t6w9z$C&F)J@NcRfUjXn";

  private ProductionSecretsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ProductionSecretsValidator();
    // Start from a fully-overridden, safe configuration for each test.
    ReflectionTestUtils.setField(validator, "jwtSecret", "unique-prod-jwt-secret");
    ReflectionTestUtils.setField(validator, "encryptionSecretPassword", "unique-prod-password");
    ReflectionTestUtils.setField(validator, "encryptionSecretSalt", "unique-prod-salt");
    ReflectionTestUtils.setField(validator, "adminPassword", "unique-prod-admin-password");
  }

  @Test
  void doesNotThrowWhenAllSecretsAreOverridden() {
    assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(validator, "validate"));
  }

  @Test
  void throwsWhenJwtSecretIsStillTheDefault() {
    ReflectionTestUtils.setField(validator, "jwtSecret", DEFAULT_JWT_SECRET);

    var exception =
        assertThrows(
            IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(validator, "validate"));
    assertTrue(exception.getMessage().contains("JWT_SECRET"));
  }

  @Test
  void throwsWhenAdminPasswordIsStillTheDefault() {
    ReflectionTestUtils.setField(validator, "adminPassword", "password");

    var exception =
        assertThrows(
            IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(validator, "validate"));
    assertTrue(exception.getMessage().contains("ADMIN_PASSWORD"));
  }

  @Test
  void throwsWhenEncryptionSecretsAreStillTheDefaults() {
    ReflectionTestUtils.setField(validator, "encryptionSecretPassword", "password");
    ReflectionTestUtils.setField(validator, "encryptionSecretSalt", "salt");

    var exception =
        assertThrows(
            IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(validator, "validate"));
    assertTrue(exception.getMessage().contains("ENCRYPTION_SECRET_PASSWORD"));
    assertTrue(exception.getMessage().contains("ENCRYPTION_SECRET_SALT"));
  }
}
