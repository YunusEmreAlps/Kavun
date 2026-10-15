package com.kavun.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the caller to hold a {@code PAGE_CODE:ACTION_CODE} permission. The page is bound on the
 * server, never taken from the request.
 *
 * <p>Put the page on the controller and, where the HTTP method does not say what the endpoint
 * does, the action on the method:
 *
 * <pre>{@code
 * @RequirePermission(page = Pages.ROLES)        // GET -> ROLES:VIEW, POST -> ROLES:CREATE, ...
 * public class RoleRestApi {
 *
 *   @PostMapping("/search")
 *   @RequirePermission(action = Actions.VIEW)   // read-only POST -> ROLES:VIEW
 *   public Page<RoleDto> search(...) { ... }
 * }
 * }</pre>
 *
 * <p>Without an explicit action, GET/HEAD require VIEW, POST CREATE, PUT/PATCH EDIT and DELETE
 * DELETE. Every page and action used here must be declared in the permission catalog, otherwise the
 * application refuses to start. See {@link PublicEndpoint} and {@link AuthenticatedEndpoint} for
 * endpoints that need no permission.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

  /**
   * The page code. On a method, falls back to the page of the controller's annotation.
   *
   * @return the page code
   */
  String page() default "";

  /**
   * The action code, only allowed on methods. Defaults to the action implied by the HTTP method.
   *
   * @return the action code
   */
  String action() default "";
}
