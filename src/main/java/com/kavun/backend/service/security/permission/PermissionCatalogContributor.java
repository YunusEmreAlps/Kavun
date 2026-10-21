package com.kavun.backend.service.security.permission;

/**
 * Declares pages and actions in the {@link PermissionCatalog}. Every module that exposes endpoints
 * protected by {@link com.kavun.annotation.RequirePermission} registers one as a Spring bean.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@FunctionalInterface
public interface PermissionCatalogContributor {

  /**
   * Adds this module's pages and actions to the catalog.
   *
   * @param catalog the catalog being built
   */
  void contribute(PermissionCatalog.Builder catalog);
}
