package com.kavun.exception.user;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Responsible for role not found exception specifically.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Role does not exist")
public class RoleNotFoundException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not
   * initialized, and may subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the
   *     {@link #getMessage()} method.
   */
  public RoleNotFoundException(String message) {
    super(message);
  }
}
