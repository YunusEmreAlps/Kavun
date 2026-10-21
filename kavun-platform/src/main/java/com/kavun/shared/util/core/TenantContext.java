package com.kavun.shared.util.core;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.ErrorConstants;
import java.util.function.Supplier;

/**
 * The tenant the current work belongs to. Hibernate scopes every query on tenant-owned entities
 * to it and stamps it on every inserted row.
 *
 * <p>Resolution order:
 *
 * <ol>
 *   <li>an explicit {@link #callAs} / {@link #runAs} block (e.g. provisioning a new tenant),
 *   <li>the tenant of the signed-in user,
 *   <li>{@link #ROOT}: no tenant filter at all. Used for work that is not done on behalf of a user:
 *       finding the user during login, scheduled jobs, startup tasks, public endpoints.
 * </ol>
 *
 * <p>Entities cannot be created in the root context without naming their tenant explicitly.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class TenantContext {

  /** Sees the data of every tenant. Never stored on a row. */
  public static final String ROOT = "*";

  /** The tenant of the platform operators; its administrators manage all tenants. */
  public static final String PLATFORM = "platform";

  private static final ThreadLocal<String> OVERRIDE = new ThreadLocal<>();

  private TenantContext() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }

  /**
   * The tenant of the current work.
   *
   * @return the tenant code, or {@link #ROOT}
   */
  public static String current() {
    String override = OVERRIDE.get();
    if (override != null) {
      return override;
    }
    var authentication = SecurityUtils.getAuthentication();
    if (SecurityUtils.isAuthenticated(authentication)
        && authentication.getPrincipal() instanceof UserDetailsBuilder user
        && user.getTenantCode() != null) {
      return user.getTenantCode();
    }
    return ROOT;
  }

  /** Whether the current work is done by (or for) the platform tenant. */
  public static boolean isPlatform() {
    return PLATFORM.equals(current());
  }

  /**
   * Runs work as the given tenant. Database sessions opened inside the block are scoped to it, so
   * start a new transaction inside the block rather than reusing an outer one.
   *
   * @param tenantCode the tenant code, {@link #ROOT}, or null to resolve the tenant from the
   *     signed-in user as if no block was active
   * @param work the work
   * @param <T> the result type
   * @return the result of the work
   */
  public static <T> T callAs(String tenantCode, Supplier<T> work) {
    String previous = OVERRIDE.get();
    if (tenantCode == null) {
      OVERRIDE.remove();
    } else {
      OVERRIDE.set(tenantCode);
    }
    try {
      return work.get();
    } finally {
      if (previous == null) {
        OVERRIDE.remove();
      } else {
        OVERRIDE.set(previous);
      }
    }
  }

  /**
   * Runs work as the given tenant; see {@link #callAs}.
   *
   * @param tenantCode the tenant code, {@link #ROOT}, or null
   * @param work the work
   */
  public static void runAs(String tenantCode, Runnable work) {
    callAs(
        tenantCode,
        () -> {
          work.run();
          return null;
        });
  }
}
