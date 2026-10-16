package com.kavun.backend.service.security.permission;

import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.user.PermissionCheckService;
import com.kavun.enums.RoleType;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.shared.util.core.TenantContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Answers whether a caller may perform an action: page permissions for endpoints, plus the rules
 * that keep non-admins from granting themselves admin rights.
 *
 * <p>Holders of {@code ROLE_ADMIN} (each tenant has its own) pass every permission check while
 * {@code security.permission.admin-bypass-enabled} is on; otherwise they need explicit grants like
 * anyone else (the permission catalog grants every page action to the {@code ROLE_ADMIN} of each
 * tenant when it is created). Platform-only pages are refused to every tenant but the platform.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class AccessControlService {

  private static final String ADMIN_AUTHORITY = RoleType.ROLE_ADMIN.getName();
  private static final Set<String> SYSTEM_ROLE_NAMES =
      Arrays.stream(RoleType.values()).map(RoleType::getName).collect(Collectors.toSet());

  private final PermissionCheckService permissionCheckService;
  private final PermissionCatalog permissionCatalog;
  private final boolean adminBypassEnabled;

  public AccessControlService(
      PermissionCheckService permissionCheckService,
      PermissionCatalog permissionCatalog,
      @Value("${security.permission.admin-bypass-enabled:false}") boolean adminBypassEnabled) {
    this.permissionCheckService = permissionCheckService;
    this.permissionCatalog = permissionCatalog;
    this.adminBypassEnabled = adminBypassEnabled;
  }

  /**
   * Whether the caller holds the permission.
   *
   * @param authentication the caller
   * @param permission the required permission
   * @return true if access is granted
   */
  public boolean hasPermission(Authentication authentication, PageActionCode permission) {
    if (!SecurityUtils.isAuthenticated(authentication)) {
      return false;
    }
    if (permissionCatalog.isPlatformOnly(permission.page()) && !TenantContext.isPlatform()) {
      return false;
    }
    if (adminBypassEnabled && isAdmin(authentication)) {
      return true;
    }
    if (!(authentication.getPrincipal() instanceof UserDetailsBuilder user)) {
      LOG.warn("Cannot check permissions for principal type {}", authentication.getPrincipal());
      return false;
    }
    return permissionCheckService.hasPermission(
        user.getId(), permission.page(), permission.action());
  }

  /**
   * Requires the current caller to hold the permission.
   *
   * @param page the page code
   * @param action the action code
   * @throws AccessDeniedException if the caller lacks it
   */
  public void checkPermission(String page, String action) {
    var code = new PageActionCode(page, action);
    if (!hasPermission(SecurityUtils.getAuthentication(), code)) {
      throw new AccessDeniedException("Missing permission " + code);
    }
  }

  /**
   * Requires the current caller to be an admin before handing out an admin role.
   *
   * @param role the role being assigned
   * @throws AccessDeniedException if a non-admin assigns the admin role
   */
  public void checkCanAssign(Role role) {
    if (ADMIN_AUTHORITY.equals(role.getName()) && !isAdmin(SecurityUtils.getAuthentication())) {
      throw new AccessDeniedException("Only administrators can assign " + ADMIN_AUTHORITY);
    }
  }

  /**
   * Requires the current caller to be an admin before changing or deleting one of the built-in
   * roles, whose names the security rules depend on.
   *
   * @param role the role being changed
   * @throws AccessDeniedException if a non-admin changes a built-in role
   */
  public void checkCanModifyRole(Role role) {
    if (SYSTEM_ROLE_NAMES.contains(role.getName()) && !isAdmin(SecurityUtils.getAuthentication())) {
      throw new AccessDeniedException("Only administrators can change built-in roles");
    }
  }

  /**
   * Requires the current caller to be an admin before changing an admin account, so that a user
   * manager cannot take an admin account over (e.g. by changing its email).
   *
   * @param targetRoleNames the role names of the user being changed
   * @throws AccessDeniedException if a non-admin changes an admin
   */
  public void checkCanManageUserWithRoles(Collection<String> targetRoleNames) {
    if (targetRoleNames.contains(ADMIN_AUTHORITY) && !isAdmin(SecurityUtils.getAuthentication())) {
      throw new AccessDeniedException("Only administrators can change administrator accounts");
    }
  }

  private static boolean isAdmin(Authentication authentication) {
    return SecurityUtils.isAuthenticated(authentication)
        && authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ADMIN_AUTHORITY::equals);
  }
}
