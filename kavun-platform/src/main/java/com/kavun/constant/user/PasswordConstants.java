package com.kavun.constant.user;

import com.kavun.constant.ErrorConstants;

/**
 * This class holds all constants used in PasswordToken implementations.
 *
 * @author Yunus Emre Alpu
 */
public final class PasswordConstants {

  /** URL Mapping Constants for forget password path. */
  public static final String PASSWORD_RESET_ROOT_MAPPING = "/password-reset";

  /** URL Mapping Constants for change password path. */
  public static final String PASSWORD_CHANGE_PATH = "/change";

  /** Constructor for Password Token Constants made private. */
  private PasswordConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
