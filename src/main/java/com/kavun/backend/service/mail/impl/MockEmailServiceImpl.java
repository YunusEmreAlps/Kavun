package com.kavun.backend.service.mail.impl;

import com.kavun.constant.EmailConstants;
import com.kavun.constant.EnvConstants;
import com.kavun.web.payload.request.mail.EmailRequest;
import com.kavun.web.payload.request.mail.HtmlEmailRequest;
import com.kavun.web.payload.response.CustomResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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
  public CustomResponse<String> sendSimpleEmail(final SimpleMailMessage simpleMailMessage) {
    try {
      sendMail(simpleMailMessage);
      String recipients = simpleMailMessage.getTo() != null ? String.join(", ", simpleMailMessage.getTo()) : "unknown";

      return CustomResponse.of(
          HttpStatus.OK,
          EmailConstants.MAIL_SUCCESS_MESSAGE,
          simpleMailMessage.getSubject() + " sent to " + recipients);

    } catch (Exception e) {
      LOG.error("Failed to send simple email", e);
      return CustomResponse.of(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to send email: " + e.getMessage(),
          null);
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

}
