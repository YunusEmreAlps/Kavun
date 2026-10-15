package com.kavun.exception;

import java.io.Serial;

/**
 * Thrown when a list filter has a value that does not fit the filtered field, e.g. {@code
 * enabled=maybe}. Answered with 400.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public class InvalidFilterException extends RuntimeException {
  @Serial private static final long serialVersionUID = -2934766208735183517L;

  public InvalidFilterException(String message) {
    super(message);
  }
}
