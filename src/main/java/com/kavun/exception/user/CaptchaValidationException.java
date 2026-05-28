package com.kavun.exception.user;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when captcha validation fails.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid captcha")
public class CaptchaValidationException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new CaptchaValidationException with the specified detail message.
   *
   * @param message the detail message
   */
  public CaptchaValidationException(final String message) {
    super(message);
  }

  /**
   * Constructs a new CaptchaValidationException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public CaptchaValidationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
