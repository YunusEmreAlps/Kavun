package com.kavun.backend.service.mail.impl;

import com.kavun.constant.EnvConstants;
import com.kavun.constant.email.EmailConstants;
import com.kavun.web.payload.request.mail.EmailRequest;
import com.kavun.web.payload.request.mail.HtmlEmailRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Class simulates the operation of email sending without a real time call.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Profile(EnvConstants.INTEGRATION_TEST)
public class MockEmailServiceImpl extends AbstractEmailServiceImpl {

  /**
   * Sends an email with the provided simple mail message object.
   *
   * @param simpleMailMessage the simple mail message.
   */
  @Override
  public void sendMail(SimpleMailMessage simpleMailMessage) {
    LOG.info(EmailConstants.SIMULATING_SENDING_AN_EMAIL);
    LOG.info("Simple Mail Message content is {}", simpleMailMessage);
    LOG.info(EmailConstants.MAIL_SUCCESS_MESSAGE);
  }

  @Override
  public String sendSimpleEmail(final SimpleMailMessage simpleMailMessage) {
    try {
      sendMail(simpleMailMessage);
      String recipients = simpleMailMessage.getTo() != null ? String.join(", ", simpleMailMessage.getTo()) : "unknown";

      return EmailConstants.MAIL_SUCCESS_MESSAGE + ": " + simpleMailMessage.getSubject() + " sent to " + recipients;

    } catch (Exception e) {
      LOG.error("Failed to send simple email", e);
      return "Failed to send email: " + e.getMessage();
    }
  }

  /**
   * Sends an email with the provided EmailRequestBuilder details.
   *
   * @param emailRequest the email format
   * @see EmailRequest
   */
  @Override
  public void sendHtmlEmail(HtmlEmailRequest emailRequest) {
    LOG.info(EmailConstants.SIMULATING_SENDING_AN_EMAIL);
    LOG.info("Email request details include: {}", emailRequest);
    LOG.info(EmailConstants.MAIL_SUCCESS_MESSAGE);
  }

  /**
   * Sends an email with the provided details and template for html with an
   * attachment.
   *
   * @param emailRequest the email format
   */
  @Override
  public void sendHtmlEmailWithAttachment(HtmlEmailRequest emailRequest) {
    LOG.info(EmailConstants.SIMULATING_SENDING_AN_EMAIL);
    LOG.info("attachments to be emailed are {}", emailRequest.getAttachments());
    LOG.info(EmailConstants.MAIL_SUCCESS_MESSAGE);
  }

  /**
   * Mock implementation of log email metrics.
   * Does nothing as MockEmailServiceImpl doesn't track metrics.
   */
  @Override
  public void logEmailMetrics() {
    LOG.debug("Mock email service - email metrics logging (no-op)");
  }

  /**
   * Mock implementation of cleanup rate limit cache.
   * Does nothing as MockEmailServiceImpl doesn't use rate limiting.
   */
  @Override
  public void cleanupRateLimitCache() {
    LOG.debug("Mock email service - rate limit cache cleanup (no-op)");
  }

}
