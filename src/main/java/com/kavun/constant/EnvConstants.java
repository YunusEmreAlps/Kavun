package com.kavun.constant;

/**
 * This class holds all profile type constants used in the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class EnvConstants {

  public static final String DOCKER = "docker";
  public static final String DEVELOPMENT = "development";
  public static final String TEST = "test";
  public static final String PRODUCTION = "production";
  public static final String INTEGRATION_TEST = "integration-test";
  public static final String INTEGRATION_TEST_CI = "integration-test-ci";

  private EnvConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
