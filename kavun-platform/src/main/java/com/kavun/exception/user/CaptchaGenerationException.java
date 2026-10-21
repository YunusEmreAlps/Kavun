package com.kavun.exception.user;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when captcha generation fails.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Failed to generate captcha")
public class CaptchaGenerationException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new CaptchaGenerationException with the specified detail message.
   *
   * @param message the detail message
   */
  public CaptchaGenerationException(final String message) {
    super(message);
  }

  /**
   * Constructs a new CaptchaGenerationException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public CaptchaGenerationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
