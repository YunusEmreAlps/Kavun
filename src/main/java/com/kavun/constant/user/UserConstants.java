package com.kavun.constant.user;

import com.kavun.constant.ErrorConstants;

/**
 * User constant provides details about user.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class UserConstants {

  // General
  public static final int DAYS_TO_ALLOW_ACCOUNT_ACTIVATION = 30;

  // Field keys
  public static final String USER_MODEL_KEY = "user";
  public static final String EMAIL = "email";
  public static final String USERNAME = "username";

  // Field sizes
  public static final int USERNAME_MIN_SIZE = 3;
  public static final int USERNAME_MAX_SIZE = 50;
  public static final int FIRSTNAME_MIN_SIZE = 2;
  public static final int FIRSTNAME_MAX_SIZE = 100;
  public static final int LASTNAME_MIN_SIZE = 2;
  public static final int LASTNAME_MAX_SIZE = 100;
  public static final int ROLE_NAME_MIN_SIZE = 3;
  public static final int ROLE_NAME_MAX_SIZE = 255;
  public static final int ROLE_DESCRIPTION_MAX_SIZE = 2000;

  // Validation messages
  public static final String BLANK_USERNAME = "Username cannot be blank";
  public static final String USERNAME_SIZE = "Username should be at least 3 and at most 50 characters";
  public static final String BLANK_EMAIL = "Email cannot be blank";
  public static final String INVALID_EMAIL = "A valid email format is required";
  public static final String BLANK_NAME = "Name cannot be blank";
  public static final String BLANK_PUBLIC_ID = "PublicId cannot be left blank";
  public static final String BLANK_PHONE = "Phone cannot be left blank";
  public static final String BLANK_USER_TYPE = "User type cannot be left blank";
  public static final String BLANK_PASSWORD = "Password cannot be left blank";
  public static final String PASSWORD_SIZE = "Password should be at least 4 characters";
  public static final String NAME_SIZE = "Name should be at least 2 and at most 100 characters";
  public static final String BLANK_FIRST_NAME = "First name cannot be blank";
  public static final String BLANK_LAST_NAME = "Last name cannot be blank";
  public static final String ROLE_DESCRIPTION_SIZE_MESSAGE = "Description should be at most 2000 characters";
  public static final String ROLE_NAME_SIZE_MESSAGE = "Role name should be at least 3 and at most 255 characters";
  public static final String PAGEABLE_MUST_NOT_BE_NULL = "Pageable must not be null";

  // User messages
  public static final String USER_PERSISTED_SUCCESSFULLY = "User successfully persisted {}";
  public static final String USER_MUST_NOT_BE_NULL = "User must not be null";
  public static final String USER_DTO_MUST_NOT_BE_NULL = "UserDto must not be null";
  public static final String USER_ALREADY_EXIST = "Email {} already exist and nothing will be done";
  public static final String USER_NOT_FOUND = "User not found";
  public static final String USERNAME_OR_EMAIL_EXISTS = "Username or email already exist";
  public static final String USER_EXIST_BUT_NOT_ENABLED = "Email {} exists but not enabled. Returning user {}";
  public static final String USER_DETAILS_DEBUG_MESSAGE = "User details {}";
  public static final String USER_ID_MUST_NOT_BE_NULL = "User Id must not be null";
  public static final String USER_DISABLED_MESSAGE = "User is disabled";
  public static final String USER_LOCKED_MESSAGE = "User is locked";
  public static final String USER_EXPIRED_MESSAGE = "User is expired";
  public static final String USER_CREDENTIALS_EXPIRED_MESSAGE = "User credentials expired";
  public static final String USER_CREATED = "User created successfully";
  public static final String USER_ENABLED_SUCCESSFULLY = "User enabled successfully";
  public static final String USER_ENABLED_FAILED = "User enabled failed for some reason. Please try again.";
  public static final String USER_DISABLED_SUCCESSFULLY = "User disabled successfully";
  public static final String USER_DISABLED_FAILED = "User disabled failed for some reason. Please try again.";
  public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully";
  public static final String USER_UPDATED_SUCCESSFULLY = "User updated successfully";
  public static final String PROFILE_UPDATED_SUCCESSFULLY = "Profile updated successfully";
  public static final String INVALID_PROFILE_UPDATE = "Invalid profile update. Please check your input and try again.";

  // Password messages
  public static final String PASSWORD_RESET_FAILED = "Password reset failed. Please try again.";
  public static final String PASSWORD_RESET_SUCCESSFULLY = "Password reset successfully";
  public static final String PASSWORD_RESET_LINK_SENT_YOUR_EMAIL_SUCCESSFULLY = "Password reset link sent to your email successfully";
  public static final String PASSWORD_UPDATED_FAILED = "Password update failed. Please try again.";
  public static final String PASSWORD_UPDATED_SUCCESSFULLY = "Password updated successfully";

  // Identification number validation
  public static final String IDENTIFICATION_NUMBER_VALID = "Identification number is valid";
  public static final String IDENTIFICATION_NUMBER_INVALID = "Identification number is invalid";

  // API Paths
  public static final String PUBLIC_ID_PATH = "/{publicId}";
  public static final String UPDATE_PASSWORD_PATH = "/update-password";
  public static final String ENABLE_USER_PATH = "/{publicId}/enable";
  public static final String DISABLE_USER_PATH = "/{publicId}/disable";

  private UserConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
