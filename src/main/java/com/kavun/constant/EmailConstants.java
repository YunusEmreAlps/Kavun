package com.kavun.constant;

/**
 * This class holds all constants used in Email operations.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class EmailConstants {

  // Field sizes
  public static final int TITLE_MAX_SIZE = 150;
  public static final int MAIL_MAX_SIZE = 255;
  public static final int MESSAGE_MAX_SIZE = 1000;
  public static final int RESULT_MAX_SIZE = 1000;

  // Validation messages
  public static final String INVALID_EMAIL = "Invalid email format";
  public static final String BLANK_TITLE = "Title cannot be blank";
  public static final String BLANK_MAIL = "Mail cannot be blank";
  public static final String TITLE_SIZE = "Title should be at most 150 characters";
  public static final String MAIL_SIZE = "Mail should be at most 255 characters";
  public static final String MESSAGE_SIZE = "Message should be at most 1000 characters";
  public static final String RESULT_SIZE = "Result should be at most 1000 characters";

  // Messages
  public static final String URLS = "urls";
  public static final String EMAIL_LINK = "link";
  public static final String MESSAGE = "message";
  public static final String MAIL_SUCCESS_MESSAGE = "Mail successfully sent!";
  public static final String CONFIRMATION_PENDING_EMAIL_SUBJECT = "You are almost there...";
  public static final String CONFIRMATION_SUCCESS_EMAIL_SUBJECT = "Thank you for choosing us";
  public static final String PASSWORD_RESET_EMAIL_SUBJECT = "How to Reset Your Password";
  public static final String PASSWORD_RESET_SUCCESS_SUBJECT = "Password successfully updated.";
  public static final String SIMULATING_SENDING_AN_EMAIL = "Simulating sending an email...";

  // API Paths
  public static final String HOME_LINK = "home";
  public static final String OTP_TEMPLATE = "email/otp";
  public static final String ABOUT_US_LINK = "aboutUsLink";
  public static final String CONTACT_US_LINK = "contact-us";
  public static final String COPY_ABOUT_US = "/copy/about-us";

  // Templates
  public static final String EMAIL_TEMPLATE = "email/template";
  public static final String EMAIL_FOOTER_TEMPLATE = "email/footer";
  public static final String EMAIL_HEADER_TEMPLATE = "email/header";
  public static final String EMAIL_WELCOME_TEMPLATE = "email/welcome";
  public static final String EMAIL_VERIFY_TEMPLATE = "email/verify-email";
  public static final String PASSWORD_RESET_TEMPLATE = "email/reset-password";
  public static final String PASSWORD_UPDATE_TEMPLATE = "email/password-update";

  private EmailConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }
}
