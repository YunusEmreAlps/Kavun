package com.kavun.constant.user;

import com.kavun.constant.ErrorConstants;

/**
 * Profile constant provides details about user profile.
 *
 * @author Yunus Emre Alpu
 */
public final class ProfileConstants {

  /** URL Mapping Constants. */
  public static final String PROFILE_MAPPING = "/profile";

  public static final String PIC_SUM_PHOTOS_150_RANDOM = "https://picsum.photos/150/150/?random";

  private ProfileConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
