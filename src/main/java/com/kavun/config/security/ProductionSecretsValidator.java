package com.kavun.config.security;

import com.kavun.constant.EnvConstants;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application in the production profile if security-sensitive properties
 * are still set to the insecure placeholder defaults shipped in {@code application.properties}.
 *
 * <p>Those defaults exist so the app runs out of the box in development; without this guard, a
 * production deployment that forgets to set the corresponding environment variables would boot
 * silently and serve traffic with a publicly-known JWT signing secret, encryption key, and admin
 * password.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@Profile(EnvConstants.PRODUCTION)
public class ProductionSecretsValidator {

  // Mirrors the placeholder defaults in application.properties - if any of these are still
  // active in the production profile, the corresponding environment variable was never set.
  private static final String DEFAULT_JWT_SECRET =
      "u7x!A%D*G-KaPdSgVkYp2s5v8y/B?E(H+MbQeThWmZq4t6w9z$C&F)J@NcRfUjXn";
  private static final String DEFAULT_ENCRYPTION_PASSWORD = "password";
  private static final String DEFAULT_ENCRYPTION_SALT = "salt";
  private static final String DEFAULT_ADMIN_PASSWORD = "password";

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${encryption.secret.password}")
  private String encryptionSecretPassword;

  @Value("${encryption.secret.salt}")
  private String encryptionSecretSalt;

  @Value("${admin.password}")
  private String adminPassword;

  @PostConstruct
  private void validate() {
    List<String> violations = new ArrayList<>();

    if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
      violations.add("JWT_SECRET (jwt.secret)");
    }
    if (DEFAULT_ENCRYPTION_PASSWORD.equals(encryptionSecretPassword)) {
      violations.add("ENCRYPTION_SECRET_PASSWORD (encryption.secret.password)");
    }
    if (DEFAULT_ENCRYPTION_SALT.equals(encryptionSecretSalt)) {
      violations.add("ENCRYPTION_SECRET_SALT (encryption.secret.salt)");
    }
    if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
      violations.add("ADMIN_PASSWORD (admin.password)");
    }

    if (!violations.isEmpty()) {
      String message =
          "Refusing to start with the 'production' profile: the following properties are still "
              + "set to their insecure development defaults: "
              + String.join(", ", violations)
              + ". Set the corresponding environment variables to real, unique values before "
              + "deploying to production.";
      LOG.error(message);
      throw new IllegalStateException(message);
    }
  }
}
