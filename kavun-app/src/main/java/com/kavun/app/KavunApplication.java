package com.kavun.app;

import com.kavun.KavunPlatform;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * The class has the main method to get the application started. It runs the platform on its own;
 * an application with its own features is built the same way (see docs/APPLICATIONS.md).
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@Import(KavunPlatform.class)
public class KavunApplication {

  /**
   * The application's entry point.
   *
   * @param args an array of command-line arguments for the application
   */
  public static void main(final String[] args) {
    SpringApplication.run(KavunApplication.class, args);
  }
}
