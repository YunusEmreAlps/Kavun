package com.kavun.constant.user;

import com.kavun.constant.ErrorConstants;

/**
 * This class holds sign-up related URL mapping constants.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class SignUpConstants {

  /** URL Mapping Constants. */
  public static final String SIGN_UP_MAPPING = "/sign-up";

  public static final String SIGN_UP_VERIFY_MAPPING = "/verify";

  private SignUpConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
