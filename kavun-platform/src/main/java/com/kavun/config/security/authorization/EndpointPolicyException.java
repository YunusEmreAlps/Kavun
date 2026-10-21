package com.kavun.config.security.authorization;

import java.io.Serial;

/**
 * Thrown when an endpoint's access annotations are missing or contradictory.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public class EndpointPolicyException extends IllegalStateException {

  @Serial private static final long serialVersionUID = 6403719266468711053L;

  public EndpointPolicyException(String message) {
    super(message);
  }
}
