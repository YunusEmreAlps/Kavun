package com.kavun.constant;

/**
 * This class holds all constants used by the operations available to the ADMIN.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class AdminConstants {

  /** Admin Controller URI Mappings. */
  public static final String API_V1_USERS_ROOT_URL = "/api/v1/users";

  public static final String API_V1_ROLE_ROOT_URL = "/api/v1/roles";

  private AdminConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
