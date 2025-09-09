package com.kavun.backend.service.mail.impl;

import com.kavun.backend.persistent.domain.email.Email;
import com.kavun.backend.persistent.repository.EmailRepository;
import com.kavun.config.properties.SystemProperties;
import com.kavun.constant.EnvConstants;
import com.kavun.constant.email.EmailConstants;
import com.kavun.web.payload.request.mail.HtmlEmailRequest;
import com.kavun.web.payload.response.CustomResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * SmtpEmailServiceImpl Class has the operation of email sending in a real time.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @see com.kavun.backend.service.mail.EmailService
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile({ EnvConstants.DEVELOPMENT, EnvConstants.TEST, EnvConstants.PRODUCTION })
public class SmtpEmailServiceImpl extends AbstractEmailServiceImpl {

  // private final MailService mailService;
  private final SystemProperties systemProps;
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailRepository emailRepository;

  /**
   * Sends an email with the provided simple mail message object.
   * Enhanced with better error handling and guaranteed app stability.
   */
  @Override
  @Async("emailTaskExecutor")
  public void sendMail(final SimpleMailMessage simpleMailMessage) {
    String requestId = generateRequestId();

    try {
      // Validate input
      if (!isValidMailMessage(simpleMailMessage)) {
        LOG.warn("Invalid mail message provided. RequestId: {}", requestId);
        saveFailedMailToDatabase(simpleMailMessage, requestId, "Invalid mail message");
        return; // Don't throw - just return to keep app working
      }

      // Configure SSL trust for IP-based SMTP servers
      configureSSLTrust();

      // Send email
      mailSender.send(simpleMailMessage);

      // Save success to database
      saveMailStatusToDatabase(simpleMailMessage, requestId, "Email sent successfully", true);
      LOG.info("Email sent successfully. RequestId: {}", requestId);

    } catch (Exception e) {
      // Log error but don't throw - keep app working
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send email. RequestId: {}, Error: {}", requestId, errorMessage, e);

      // Save failure to database
      saveFailedMailToDatabase(simpleMailMessage, requestId, errorMessage);
    }
  }

  @Override
  @Async("emailTaskExecutor")
  public CustomResponse<String> sendSimpleEmail(final SimpleMailMessage simpleMailMessage) {
    String requestId = generateRequestId();

    try {
      // Validate input
      if (!isValidMailMessage(simpleMailMessage)) {
        String error = "Invalid mail message provided";
        LOG.warn("{}. RequestId: {}", error, requestId);
        return CustomResponse.of(HttpStatus.BAD_REQUEST, error, null);
      }

      // Configure and send
      configureSSLTrust();
      mailSender.send(simpleMailMessage);

      // Save success
      saveMailStatusToDatabase(simpleMailMessage, requestId, "Email sent successfully", true);

      String recipients = getRecipientsAsString(simpleMailMessage);
      String successMessage = String.format("Email '%s' sent to %s",
          simpleMailMessage.getSubject(), recipients);

      LOG.info("Simple email sent successfully. RequestId: {}", requestId);
      return CustomResponse.of(HttpStatus.OK, EmailConstants.MAIL_SUCCESS_MESSAGE, successMessage);

    } catch (Exception e) {
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send simple email. RequestId: {}, Error: {}", requestId, errorMessage, e);

      // Save failure
      saveFailedMailToDatabase(simpleMailMessage, requestId, errorMessage);

      return CustomResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to send email: " + errorMessage, null);
    }
  }

  @Override
  @Async("emailTaskExecutor")
  public void sendHtmlEmail(final HtmlEmailRequest emailRequest) {
    String requestId = extractOrGenerateRequestId(emailRequest);

    try {
      // Validate input
      if (!isValidHtmlEmailRequest(emailRequest)) {
        LOG.warn("Invalid HTML email request provided. RequestId: {}", requestId);
        saveFailedHtmlEmailToDatabase(emailRequest, requestId, "Invalid email request");
        return; // Don't throw - just return to keep app working
      }

      configureSSLTrust();
      // logEmail(emailRequest);

      // Prepare and send email
      MimeMessage mimeMessage = prepareMimeMessage(emailRequest);
      mailSender.send(mimeMessage);

      // Save success to database
      String htmlBody = getEmailBodySafely(mimeMessage);
      saveHtmlEmailToDatabase(emailRequest, requestId, htmlBody, "Email sent successfully", true);

      LOG.info("HTML email sent successfully. RequestId: {}", requestId);

    } catch (Exception e) {
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send HTML email. RequestId: {}, Error: {}", requestId, errorMessage, e);

      // Save failure to database
      saveFailedHtmlEmailToDatabase(emailRequest, requestId, errorMessage);
    }
  }

  @Override
  @Async("emailTaskExecutor")
  public void sendHtmlEmailWithAttachment(final HtmlEmailRequest emailRequest) {
    String requestId = extractOrGenerateRequestId(emailRequest);

    try {
      // Validate input
      if (!isValidHtmlEmailRequestWithAttachments(emailRequest)) {
        LOG.warn("Invalid HTML email request with attachments. RequestId: {}", requestId);
        saveFailedHtmlEmailToDatabase(emailRequest, requestId, "Invalid email request or attachments");
        return; // Don't throw - just return to keep app working
      }

      configureSSLTrust();
      // logEmail(emailRequest);

      // Prepare and send email
      MimeMessage mimeMessage = prepareMimeMessage(emailRequest);
      mailSender.send(mimeMessage);

      // Save success to database
      String htmlBody = getEmailBodySafely(mimeMessage);
      saveHtmlEmailToDatabase(emailRequest, requestId, htmlBody, "Email with attachments sent successfully", true);

      LOG.info("HTML email with attachments sent successfully. RequestId: {}", requestId);

    } catch (Exception e) {
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send HTML email with attachment. RequestId: {}, Error: {}", requestId, errorMessage, e);

      // Save failure to database
      saveFailedHtmlEmailToDatabase(emailRequest, requestId, errorMessage);
    }
  }

  // =========================================================================
  // VALIDATION METHODS
  // =========================================================================

  /**
   * Validates simple mail message
   */
  private boolean isValidMailMessage(SimpleMailMessage simpleMailMessage) {
    if (simpleMailMessage == null) {
      return false;
    }

    String[] recipients = simpleMailMessage.getTo();
    return recipients != null && recipients.length > 0 &&
        Arrays.stream(recipients).anyMatch(StringUtils::isNotBlank);
  }

  /**
   * Validates HTML email request
   */
  private boolean isValidHtmlEmailRequest(HtmlEmailRequest emailRequest) {
    if (emailRequest == null) {
      return false;
    }

    // Check if we have at least one valid recipient
    boolean hasValidRecipient = false;

    if (StringUtils.isNotBlank(emailRequest.getTo())) {
      hasValidRecipient = true;
    } else if (emailRequest.getReceiver() != null &&
        StringUtils.isNotBlank(emailRequest.getReceiver().getEmail())) {
      hasValidRecipient = true;
    } else if (CollectionUtils.isNotEmpty(emailRequest.getRecipients())) {
      hasValidRecipient = emailRequest.getRecipients().stream()
          .anyMatch(StringUtils::isNotBlank);
    }

    return hasValidRecipient && StringUtils.isNotBlank(emailRequest.getTemplate());
  }

  /**
   * Validates HTML email request with attachments
   */
  private boolean isValidHtmlEmailRequestWithAttachments(HtmlEmailRequest emailRequest) {
    if (!isValidHtmlEmailRequest(emailRequest)) {
      return false;
    }

    // Validate attachments if present
    if (CollectionUtils.isNotEmpty(emailRequest.getAttachments())) {
      return emailRequest.getAttachments().stream()
          .allMatch(file -> file != null && file.exists() && file.canRead());
    }

    return true;
  }

  // =========================================================================
  // UTILITY METHODS
  // =========================================================================

  /**
   * Generates a unique request ID
   */
  private String generateRequestId() {
    return "EMAIL_" + System.currentTimeMillis() + "_" +
        java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999);
  }

  /**
   * Extracts request ID from email request or generates one
   */
  private String extractOrGenerateRequestId(HtmlEmailRequest emailRequest) {
    if (emailRequest != null && emailRequest.getContext() != null &&
        emailRequest.getContext().getVariable("requestId") != null) {
      return emailRequest.getContext().getVariable("requestId").toString();
    }
    return generateRequestId();
  }

  /**
   * Gets recipients as a comma-separated string
   */
  private String getRecipientsAsString(SimpleMailMessage simpleMailMessage) {
    if (simpleMailMessage.getTo() != null) {
      return String.join(", ", simpleMailMessage.getTo());
    }
    return "unknown";
  }

  /**
   * Safely extracts email body from MimeMessage
   */
  private String getEmailBodySafely(MimeMessage mimeMessage) {
    try {
      Object content = mimeMessage.getContent();
      return content != null ? content.toString() : "Email content could not be extracted";
    } catch (Exception e) {
      LOG.warn("Could not extract email body: {}", e.getMessage());
      return "Email body extraction failed";
    }
  }

  /**
   * Configures SSL trust settings for IP-based SMTP servers.
   * Enhanced with error handling.
   */
  private void configureSSLTrust() {
    try {
      if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
        var props = javaMailSender.getJavaMailProperties();
        String host = javaMailSender.getHost();

        if (StringUtils.isNotBlank(host)) {
          // Configure SSL trust for the specific host
          props.put("mail.smtp.ssl.trust", host);
          props.put("mail.smtp.ssl.checkserveridentity", "false");
          props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

          // Enable STARTTLS for secure connections
          props.put("mail.smtp.starttls.enable", "true");

          // Connection timeouts
          props.put("mail.smtp.connectiontimeout", "10000");
          props.put("mail.smtp.timeout", "10000");
          props.put("mail.smtp.writetimeout", "10000");

          LOG.debug("Configured SSL trust for SMTP host: {}", host);
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to configure SSL trust: {}", e.getMessage());
      // Don't throw - this shouldn't break the app
    }
  }

  /**
   * Enhanced error message handling
   */
  private String getDetailedErrorMessage(Exception e) {
    if (e instanceof MailSendException) {
      String message = e.getMessage();
      if (message.contains("SSLHandshakeException")) {
        return "SSL Certificate validation failed - check SMTP SSL configuration";
      } else if (message.contains("Could not connect")) {
        return "Could not connect to SMTP server - check host and port configuration";
      } else if (message.contains("Authentication")) {
        return "SMTP Authentication failed - check username and password";
      } else if (message.contains("550")) {
        return "Email rejected by server - invalid recipient address";
      } else if (message.contains("timeout")) {
        return "Email sending timeout - check network connectivity";
      }
    }

    if (e instanceof MessagingException) {
      return "Email message formatting error: " + e.getMessage();
    }

    return StringUtils.defaultIfBlank(e.getMessage(), "Unknown email sending error");
  }

  /**
   * Prepares a MimeMessage with provided EmailFormat.
   * Enhanced with better error handling.
   */
  private MimeMessage prepareMimeMessage(final HtmlEmailRequest emailFormat)
      throws MessagingException, UnsupportedEncodingException, FileNotFoundException {

    // Ensure context is not null
    Context context = emailFormat.getContext();
    if (context == null) {
      context = new Context();
      emailFormat.setContext(context);
    }

    // Ensure URLs are in context
    ensureUrlsInContext(emailFormat, context);

    var withAttachment = CollectionUtils.isNotEmpty(emailFormat.getAttachments());

    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, withAttachment, StandardCharsets.UTF_8.name());

    // Set recipients
    setRecipientsForMimeMessage(emailFormat, helper);

    helper.setSentDate(new Date());

    // Process template
    String body = templateEngine.process(emailFormat.getTemplate(), emailFormat.getContext());
    helper.setText(body, true);

    // Set subject with fallback
    String subject = StringUtils.defaultIfBlank(emailFormat.getSubject(), "No Subject");
    helper.setSubject(subject);

    // Set sender information
    setFromAndReplyTo(emailFormat, helper);

    // Add attachments if present
    if (withAttachment) {
      addAttachments(emailFormat, helper);
    }

    return mimeMessage;
  }

  /**
   * Sets recipients for MimeMessage with proper validation
   */
  private void setRecipientsForMimeMessage(HtmlEmailRequest emailFormat, MimeMessageHelper helper)
      throws MessagingException {

    // Primary recipient
    if (StringUtils.isNotBlank(emailFormat.getTo())) {
      helper.setTo(emailFormat.getTo());
    } else if (emailFormat.getReceiver() != null &&
        StringUtils.isNotBlank(emailFormat.getReceiver().getEmail())) {
      helper.setTo(emailFormat.getReceiver().getEmail());
    }

    // Additional recipients as CC
    if (CollectionUtils.isNotEmpty(emailFormat.getRecipients())) {
      List<String> validRecipients = emailFormat.getRecipients().stream()
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList());

      if (!validRecipients.isEmpty()) {
        helper.setCc(validRecipients.toArray(new String[0]));
      }
    }
  }

  // =========================================================================
  // DATABASE SAVE METHODS (Enhanced)
  // =========================================================================

  /**
   * Enhanced save method with better error handling
   */
  private void saveMailStatusToDatabase(SimpleMailMessage simpleMailMessage, String requestId,
      String result, boolean status) {
    try {
      Email email = new Email();
      email.setTitle(StringUtils.defaultIfBlank(simpleMailMessage.getSubject(), "No Subject"));
      email.setMail(getRecipientsAsString(simpleMailMessage));
      email.setMessage(StringUtils.defaultIfBlank(simpleMailMessage.getText(), ""));
      email.setResult(result);
      email.setBodyHtml(false);
      email.setStatus(status);

      emailRepository.save(email);

    } catch (Exception e) {
      LOG.error("Failed to save email status to database. RequestId: {}", requestId, e);
      // Don't re-throw - database issues shouldn't break email sending
    }
  }

  /**
   * Saves failed email attempts to database
   */
  private void saveFailedMailToDatabase(SimpleMailMessage simpleMailMessage, String requestId, String errorMessage) {
    saveMailStatusToDatabase(simpleMailMessage, requestId, "FAILED: " + errorMessage, false);
  }

  /**
   * Enhanced HTML email database save
   */
  private void saveHtmlEmailToDatabase(HtmlEmailRequest emailRequest, String requestId,
      String htmlBody, String result, boolean status) {
    try {
      Email email = new Email();
      email.setTitle(StringUtils.defaultIfBlank(emailRequest.getSubject(), "No Subject"));
      email.setMail(getHtmlEmailRecipients(emailRequest));
      email.setMessage(StringUtils.defaultIfBlank(htmlBody, "HTML content not available"));
      email.setResult(result);
      email.setBodyHtml(true);
      email.setStatus(status);

      emailRepository.save(email);
      LOG.debug("Saved HTML email to database. RequestId: {}", requestId);

    } catch (Exception e) {
      LOG.error("Failed to save HTML email details to database. RequestId: {}", requestId, e);
      // Don't re-throw - database issues shouldn't break email sending
    }
  }

  /**
   * Saves failed HTML email attempts to database
   */
  private void saveFailedHtmlEmailToDatabase(HtmlEmailRequest emailRequest, String requestId, String errorMessage) {
    saveHtmlEmailToDatabase(emailRequest, requestId, "Email sending failed", "FAILED: " + errorMessage, false);
  }

  /**
   * Gets all recipients from HTML email request
   */
  private String getHtmlEmailRecipients(HtmlEmailRequest emailRequest) {
    List<String> recipients = new ArrayList<>();

    if (StringUtils.isNotBlank(emailRequest.getTo())) {
      recipients.add(emailRequest.getTo());
    }

    if (emailRequest.getReceiver() != null &&
        StringUtils.isNotBlank(emailRequest.getReceiver().getEmail())) {
      recipients.add(emailRequest.getReceiver().getEmail());
    }

    if (CollectionUtils.isNotEmpty(emailRequest.getRecipients())) {
      recipients.addAll(emailRequest.getRecipients().stream()
          .filter(StringUtils::isNotBlank)
          .collect(Collectors.toList()));
    }

    return recipients.isEmpty() ? "unknown" : String.join(", ", recipients);
  }

  /**
   * Ensures URLs are properly set in the context.
   */
  private void ensureUrlsInContext(HtmlEmailRequest emailFormat, Context context) {
    Map<String, String> urls = emailFormat.getUrls();
    if (urls == null) {
      urls = new HashMap<>();
      emailFormat.setUrls(urls);
    }

    // Add default URLs if they don't exist
    urls.putIfAbsent("imageUrl", "https://asset.turktelekom.com.tr/SiteAssets/images/logo-mobile.svg");
    urls.putIfAbsent("portalUrl", "https://verimerkezikavun.turktelekom.com.tr");

    // Set URLs in context
    context.setVariable(EmailConstants.URLS, urls);

    // Set individual URL variables for easier template access
    urls.forEach((key, value) -> {
      if (!context.containsVariable(key)) {
        context.setVariable(key, value);
      }
    });
  }

  /**
   * Sets the 'from' and 'reply-to' addresses for the email.
   *
   * @param emailFormat
   * @param helper
   * @throws UnsupportedEncodingException
   * @throws MessagingException
   */
  private void setFromAndReplyTo(final HtmlEmailRequest emailFormat, final MimeMessageHelper helper)
      throws UnsupportedEncodingException, MessagingException {

    InternetAddress internetAddress;
    if (Objects.nonNull(emailFormat.getFrom()) && Objects.nonNull(emailFormat.getSender())) {
      internetAddress = new InternetAddress(emailFormat.getFrom(), emailFormat.getSender().getFirstName());
    } else if (Objects.nonNull(emailFormat.getFrom())) {
      internetAddress = new InternetAddress(emailFormat.getFrom(), systemProps.getName());
    } else {
      internetAddress = new InternetAddress(systemProps.getEmail(), systemProps.getName());
    }

    helper.setFrom(String.valueOf(internetAddress));
    helper.setReplyTo(internetAddress);

    if (Objects.nonNull(emailFormat.getReceiver())) {
      internetAddress = new InternetAddress(emailFormat.getTo(), emailFormat.getReceiver().getFirstName());
      helper.setTo(internetAddress);
    }
    helper.setTo(internetAddress);
  }

  /**
   * Logs email sending information.
   * emailRequest CAN be HtmlEmailRequest, SimpleMailMessage, etc.
   */
  private void logEmail(Object emailRequest) {
    if (LOG.isDebugEnabled() && emailRequest != null) {
      LOG.info("=== EMAIL SEND ATTEMPT ===");
      if (emailRequest instanceof HtmlEmailRequest) {
        HtmlEmailRequest htmlEmailRequest = (HtmlEmailRequest) emailRequest;
        LOG.info("From: {}", htmlEmailRequest.getFrom());
        LOG.info("To: {}", String.join(", ", htmlEmailRequest.getTo()));
        LOG.info("Subject: {}", htmlEmailRequest.getSubject());
      } else if (emailRequest instanceof SimpleMailMessage) {
        SimpleMailMessage simpleMailMessage = (SimpleMailMessage) emailRequest;
        LOG.info("From: {}", simpleMailMessage.getFrom());
        LOG.info("To: {}", String.join(", ", simpleMailMessage.getTo()));
        LOG.info("Subject: {}", simpleMailMessage.getSubject());
      }
      LOG.info("========================");
    }
  }

}
