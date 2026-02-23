package com.kavun.constant;

/**
 * Constants used in the authentication process.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public class AuthConstants {

  // One Time Password (OTP) constants
  public static final String BLANK_OTP_DELIVERY_METHOD = "OTP delivery method cannot be left blank";
  public static final String INVALID_OTP_DELIVERY_METHOD = "Invalid OTP delivery method";
  public static final String OTP_GENERATED = "OTP generated successfully.";
  public static final String OTP_VERIFIED = "OTP verified successfully.";
  public static final String OTP_NOT_VERIFIED = "Invalid OTP. Please try again.";
  public static final String OTP_EXPIRED = "OTP has expired. Please request a new one.";
  public static final String OTP_MAX_ATTEMPTS =
      "Maximum attempts reached. Please request a new OTP.";
  public static final String OTP_SENT = "OTP sent successfully.";
  public static final String OTP_GENERATION_FAILED = "Unable to generate OTP. Please try again.";
  public static final String OTP_VALIDATION_FAILED =
      "OTP validation failed. Please check the code.";
  public static final String OTP_DELETED = "OTP deleted successfully.";
  public static final String OTP_NOT_FOUND = "OTP not found.";
  public static final String USER_HAS_NO_OTP_DELIVERY_METHOD = "User has no OTP delivery method configured.";
  public static final String OTP_SENT_SUCCESSFULLY = "OTP sent successfully.";

  private AuthConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
