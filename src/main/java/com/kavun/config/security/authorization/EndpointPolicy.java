package com.kavun.config.security.authorization;

import com.kavun.backend.service.security.permission.PageActionCode;

/**
 * Who may call an API endpoint, as declared by its access annotations.
 *
 * @param access the kind of access required
 * @param permission for {@link Access#PERMISSION}, the required permission; null otherwise
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public record EndpointPolicy(Access access, PageActionCode permission) {

  public static final EndpointPolicy PUBLIC = new EndpointPolicy(Access.PUBLIC, null);
  public static final EndpointPolicy AUTHENTICATED = new EndpointPolicy(Access.AUTHENTICATED, null);

  /** The kind of access an endpoint requires. */
  public enum Access {
    PUBLIC,
    AUTHENTICATED,
    PERMISSION
  }
}
