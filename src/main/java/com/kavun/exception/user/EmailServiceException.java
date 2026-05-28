package com.kavun.exception.user;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when email service operations fail.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Email service error")
public class EmailServiceException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new EmailServiceException with the specified detail message.
   *
   * @param message the detail message
   */
  public EmailServiceException(final String message) {
    super(message);
  }

  /**
   * Constructs a new EmailServiceException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public EmailServiceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
