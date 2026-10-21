package com.kavun.backend.service.mail.impl;

import com.kavun.exception.user.EmailServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Thin gateway around {@link JavaMailSender} that isolates the actual SMTP network call behind a
 * {@code smtpEmail} circuit breaker. Kept as a separate bean (rather than annotating methods on
 * {@link SmtpEmailServiceImpl} directly) so the breaker is invoked through the Spring AOP proxy
 * instead of via same-class self-invocation, which Spring AOP cannot intercept.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MailDeliveryGateway {

  private final JavaMailSender mailSender;

  /**
   * Sends a simple mail message, protected by the {@code smtpEmail} circuit breaker.
   *
   * @param message the message to send
   */
  @CircuitBreaker(name = "smtpEmail", fallbackMethod = "sendFallback")
  void send(final SimpleMailMessage message) {
    mailSender.send(message);
  }

  /**
   * Sends a MIME message (HTML email, with or without attachments), protected by the
   * {@code smtpEmail} circuit breaker.
   *
   * @param message the message to send
   */
  @CircuitBreaker(name = "smtpEmail", fallbackMethod = "sendMimeFallback")
  void send(final MimeMessage message) {
    mailSender.send(message);
  }

  private void sendFallback(final SimpleMailMessage message, final Throwable t) {
    LOG.error("SMTP send circuit breaker fallback triggered: {}", t.getMessage(), t);
    throw new EmailServiceException("Email delivery is currently unavailable. Please try again later.", t);
  }

  private void sendMimeFallback(final MimeMessage message, final Throwable t) {
    LOG.error("SMTP send (MIME) circuit breaker fallback triggered: {}", t.getMessage(), t);
    throw new EmailServiceException("Email delivery is currently unavailable. Please try again later.", t);
  }
}
