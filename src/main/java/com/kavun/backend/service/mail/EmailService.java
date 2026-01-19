package com.kavun.backend.service.mail;

import com.kavun.exception.InvalidServiceRequestException;
import com.kavun.shared.dto.UserDto;
import com.kavun.web.payload.request.mail.EmailRequest;
import com.kavun.web.payload.request.mail.FeedbackRequest;
import com.kavun.web.payload.request.mail.HtmlEmailRequest;

import org.springframework.mail.SimpleMailMessage;

/**
 * Provides operations on sending emails within the application.
 * Enhanced with async tracking and bulk operations.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
public interface EmailService {

  /**
   * Sends an email with the provided simple mail message object.
   * Asynchronous execution.
   *
   * @param simpleMailMessage the simple mail message.
   */
  void sendMail(SimpleMailMessage simpleMailMessage);

  /**
   * Sends an email with the provided simple mail message object.
   * Returns tracking information.
   *
   * @param simpleMailMessage the simple mail message.
   * @return a String message indicating the result of the email sending operation.
   */
  String sendSimpleEmail(final SimpleMailMessage simpleMailMessage);

  /**
   * Sends an email with the provided EmailRequestBuilder details.
   * Asynchronous execution.
   *
   * @param emailRequest the email format
   * @see EmailRequest
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendHtmlEmail(HtmlEmailRequest emailRequest);

  /**
   * Sends an email with the provided details and template for html with an
   * attachment. Asynchronous execution.
   *
   * @param emailRequest the email format
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendHtmlEmailWithAttachment(HtmlEmailRequest emailRequest);

  /**
   * Sends an email given a feedback Pojo.
   *
   * @param feedbackRequestBuilder the feedback pojo.
   * @see FeedbackRequest
   * @throws InvalidServiceRequestException if the feedback request is invalid
   */
  void sendMailWithFeedback(FeedbackRequest feedbackRequestBuilder);

  /**
   * Sends an email to the provided user to verify account.
   *
   * @param userDto the user
   * @param token   the token
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendAccountVerificationEmail(UserDto userDto, String token);

  /**
   * Sends an email to the provided user to confirm account activation.
   *
   * @param userDto the user
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendAccountConfirmationEmail(UserDto userDto);

  /**
   * Sends an email to the provided user to reset password.
   *
   * @param userDto the user
   * @param token   the password token
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendPasswordResetEmail(UserDto userDto, String token);

  /**
   * Send password reset confirmation email to user.
   *
   * @param userDto the user
   * @throws InvalidServiceRequestException if the email request is invalid
   */
  void sendPasswordResetConfirmationEmail(final UserDto userDto);

  /**
   * Scheduled task to log email service metrics.
   * This method is called periodically to report email service statistics.
   */
  void logEmailMetrics();

  /**
   * Scheduled task to clean up old rate limit cache entries.
   * This method is called periodically to remove expired rate limit data.
   */
  void cleanupRateLimitCache();
}
