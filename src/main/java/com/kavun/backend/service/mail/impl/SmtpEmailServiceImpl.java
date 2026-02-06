package com.kavun.backend.service.mail.impl;

import com.kavun.backend.persistent.domain.email.Email;
import com.kavun.backend.persistent.repository.EmailRepository;
import com.kavun.config.properties.SystemProperties;
import com.kavun.constant.EnvConstants;
import com.kavun.constant.email.EmailConstants;
import com.kavun.web.payload.request.mail.HtmlEmailRequest;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Enhanced SmtpEmailServiceImpl with async processing, rate limiting, and
 * monitoring.
 * Optimized for high-throughput email delivery with proper resource management.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @see com.kavun.backend.service.mail.EmailService
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile({ EnvConstants.DEVELOPMENT, EnvConstants.TEST, EnvConstants.PRODUCTION })
public class SmtpEmailServiceImpl extends AbstractEmailServiceImpl {

  private final SystemProperties systemProps;
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final EmailRepository emailRepository;

  // =========================================================================
  // MONITORING & METRICS
  // =========================================================================

  private final AtomicLong totalEmailsSent = new AtomicLong(0);
  private final AtomicLong totalEmailsFailed = new AtomicLong(0);
  private final AtomicInteger currentlyProcessing = new AtomicInteger(0);
  private final ConcurrentHashMap<String, Long> hourlyEmailCount = new ConcurrentHashMap<>();

  // Rate limiting: max emails per hour per recipient
  private static final int MAX_EMAILS_PER_HOUR = 100;
  private static final long HOUR_IN_MILLIS = 3600000L;

  @PostConstruct
  public void init() {
    LOG.info("Initializing SMTP Email Service with monitoring...");
    LOG.info("Rate limit: {} emails per hour per recipient", MAX_EMAILS_PER_HOUR);
  }

  /**
   * Scheduled task to log email metrics every hour
   */
  @Scheduled(fixedRate = 3600000) // Every hour
  public void logEmailMetrics() {
    LOG.info("=== EMAIL SERVICE METRICS ===");
    LOG.info("Total emails sent: {}", totalEmailsSent.get());
    LOG.info("Total emails failed: {}", totalEmailsFailed.get());
    LOG.info("Currently processing: {}", currentlyProcessing.get());
    LOG.info("Success rate: {}%", calculateSuccessRate());
    LOG.info("============================");
  }

  /**
   * Scheduled task to clean up old rate limit entries
   */
  @Scheduled(fixedRate = 1800000) // Every 30 minutes
  public void cleanupRateLimitCache() {
    long now = System.currentTimeMillis();
    hourlyEmailCount.entrySet().removeIf(entry -> now - entry.getValue() > HOUR_IN_MILLIS);
    LOG.debug("Cleaned up rate limit cache. Remaining entries: {}", hourlyEmailCount.size());
  }

  private double calculateSuccessRate() {
    long total = totalEmailsSent.get() + totalEmailsFailed.get();
    if (total == 0)
      return 100.0;
    return (totalEmailsSent.get() * 100.0) / total;
  }

  /**
   * Check if recipient has exceeded rate limit
   */
  private boolean isRateLimited(String recipient) {
    String key = "rate_" + recipient + "_" + (System.currentTimeMillis() / HOUR_IN_MILLIS);
    Long count = hourlyEmailCount.get(key);
    return count != null && count >= MAX_EMAILS_PER_HOUR;
  }

  /**
   * Increment rate limit counter for recipient
   */
  private void incrementRateLimit(String recipient) {
    String key = "rate_" + recipient + "_" + (System.currentTimeMillis() / HOUR_IN_MILLIS);
    hourlyEmailCount.merge(key, 1L, Long::sum);
  }

  /**
   * Sends an email with the provided simple mail message object.
   * Enhanced with rate limiting, metrics tracking, and better error handling.
   */
  @Override
  @Async("emailTaskExecutor")
  public void sendMail(final SimpleMailMessage simpleMailMessage) {
    String requestId = generateRequestId();
    currentlyProcessing.incrementAndGet();
    long startTime = System.currentTimeMillis();

    try {
      // Validate input
      if (!isValidMailMessage(simpleMailMessage)) {
        LOG.warn("Invalid mail message provided. RequestId: {}", requestId);
        saveFailedMailToDatabase(simpleMailMessage, requestId, "Invalid mail message");
        totalEmailsFailed.incrementAndGet();
        return;
      }

      // Rate limiting check
      String recipient = simpleMailMessage.getTo()[0];
      if (isRateLimited(recipient)) {
        LOG.warn("Rate limit exceeded for recipient: {}. RequestId: {}", recipient, requestId);
        saveFailedMailToDatabase(simpleMailMessage, requestId, "Rate limit exceeded");
        totalEmailsFailed.incrementAndGet();
        return;
      }

      // Configure SSL trust for IP-based SMTP servers
      configureSSLTrust();

      // Send email
      mailSender.send(simpleMailMessage);

      // Increment counters
      incrementRateLimit(recipient);
      totalEmailsSent.incrementAndGet();

      // Save success to database
      saveMailStatusToDatabase(simpleMailMessage, requestId, "Email sent successfully", true);

      long duration = System.currentTimeMillis() - startTime;
      LOG.info("Email sent successfully. RequestId: {}, Duration: {}ms", requestId, duration);

    } catch (Exception e) {
      totalEmailsFailed.incrementAndGet();
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send email. RequestId: {}, Error: {}", requestId, errorMessage, e);
      saveFailedMailToDatabase(simpleMailMessage, requestId, errorMessage);
    } finally {
      currentlyProcessing.decrementAndGet();
    }
  }

  @Override
  @Async("emailTaskExecutor")
  public String sendSimpleEmail(final SimpleMailMessage simpleMailMessage) {
    String requestId = generateRequestId();

    try {
      // Validate input
      if (!isValidMailMessage(simpleMailMessage)) {
        String error = "Invalid mail message provided";
        LOG.warn("{}. RequestId: {}", error, requestId);
        return error;
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
      return EmailConstants.MAIL_SUCCESS_MESSAGE + ": " + successMessage;

    } catch (Exception e) {
      String errorMessage = getDetailedErrorMessage(e);
      LOG.error("Failed to send simple email. RequestId: {}, Error: {}", requestId, errorMessage, e);

      // Save failure
      saveFailedMailToDatabase(simpleMailMessage, requestId, errorMessage);

      return "Failed to send email: " + errorMessage;
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
    if (emailRequest == null) {
      return "unknown";
    }

    try {
      Set<String> uniqueRecipients = new LinkedHashSet<>();

      if (StringUtils.isNotBlank(emailRequest.getTo())) {
        String email = emailRequest.getTo().trim().toLowerCase();
        if (isValidEmailFormat(email)) {
          uniqueRecipients.add(email);
        }
      }

      if (emailRequest.getReceiver() != null &&
          StringUtils.isNotBlank(emailRequest.getReceiver().getEmail())) {
        String email = emailRequest.getReceiver().getEmail().trim().toLowerCase();
        if (isValidEmailFormat(email)) {
          uniqueRecipients.add(email);
        }
      }

      // Add additional recipients with validation
      if (CollectionUtils.isNotEmpty(emailRequest.getRecipients())) {
        emailRequest.getRecipients().stream()
            .filter(StringUtils::isNotBlank)
            .map(email -> email.trim().toLowerCase())
            .filter(this::isValidEmailFormat)
            .forEach(uniqueRecipients::add);
      }

      if (uniqueRecipients.isEmpty()) {
        LOG.warn("No valid recipients found in HTML email request");
        return "no-valid-recipients";
      }

      String result = String.join(", ", uniqueRecipients);
      LOG.debug("Deduplicated HTML email recipients: {}", result);
      return result;

    } catch (Exception e) {
      LOG.error("Error processing HTML email recipients: {}", e.getMessage());
      return "error-processing-recipients";
    }
  }

  private boolean isValidEmailFormat(String email) {
    if (!StringUtils.isNotBlank(email)) {
      return false;
    }

    String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    return email.matches(emailRegex);
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
}
