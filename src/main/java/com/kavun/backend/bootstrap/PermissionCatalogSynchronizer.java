package com.kavun.backend.bootstrap;

import com.kavun.backend.persistent.domain.user.Action;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.backend.persistent.repository.PermissionRepository;
import com.kavun.backend.persistent.repository.RoleRepository;
import com.kavun.backend.service.security.permission.PermissionCatalog;
import com.kavun.backend.service.security.permission.PermissionCatalog.ActionDefinition;
import com.kavun.backend.service.security.permission.PermissionCatalog.PageDefinition;
import com.kavun.enums.EntityType;
import com.kavun.enums.RoleType;
import com.kavun.shared.util.core.TenantContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the {@link PermissionCatalog} into the database on startup, after the roles have been
 * seeded.
 *
 * <p>Only what is missing is created: rows edited by administrators (names, icons, menu order) are
 * left alone, and rows they soft deleted are not brought back. Each newly created page action is
 * granted to the {@code ROLE_ADMIN} of every tenant (platform-only pages: of the platform tenant
 * only), so administrators can use new features without manual setup even when the admin
 * permission bypass is off. Runs in the root tenant context, across all tenants.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(DatabaseSeeder.ORDER + 1)
public class PermissionCatalogSynchronizer implements ApplicationRunner {

  private final PermissionCatalog catalog;
  private final ActionRepository actionRepository;
  private final PageRepository pageRepository;
  private final PageActionRepository pageActionRepository;
  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Map<String, Action> actions = new HashMap<>();
    for (ActionDefinition definition : catalog.getActions()) {
      actions.put(
          definition.code(),
          actionRepository.findByCode(definition.code()).orElseGet(() -> createAction(definition)));
    }

    // Pages come parents first, so a page's parent is always resolved already.
    Map<String, WebPage> pages = new HashMap<>();
    for (PageDefinition definition : catalog.getPages()) {
      pages.put(
          definition.code(),
          pageRepository
              .findByCode(definition.code())
              .orElseGet(() -> createPage(definition, pages.get(definition.parentCode()))));
    }

    List<Role> adminRoles = roleRepository.findAllByName(RoleType.ROLE_ADMIN.getName());
    int created = 0;
    for (PageDefinition definition : catalog.getPages()) {
      WebPage page = pages.get(definition.code());
      for (String actionCode : definition.actionCodes()) {
        Action action = actions.get(actionCode);
        if (pageActionRepository.findByPageIdAndActionId(page.getId(), action.getId()).isEmpty()) {
          PageAction pageAction = createPageAction(page, action);
          adminRoles.forEach(role -> grant(role, pageAction));
          created++;
        }
      }
    }
    LOG.info("Permission catalog synchronized, {} new page actions", created);
  }

  /**
   * Grants every page action the role's tenant may use to a freshly provisioned admin role.
   *
   * @param adminRole the {@code ROLE_ADMIN} of a new tenant
   */
  @Transactional
  public void grantCatalogTo(Role adminRole) {
    pageActionRepository.findAllActiveWithDetails().forEach(pageAction -> grant(adminRole, pageAction));
  }

  private Action createAction(ActionDefinition definition) {
    var action = new Action();
    action.setCode(definition.code());
    action.setName(definition.name());
    action.setType(definition.type());
    return actionRepository.save(action);
  }

  private WebPage createPage(PageDefinition definition, WebPage parent) {
    var page = new WebPage();
    page.setCode(definition.code());
    page.setName(definition.name());
    page.setUrl(definition.url());
    page.setIcon(definition.icon());
    page.setDescription(definition.description());
    page.setParent(parent);
    page.setShowInMenu(definition.showInMenu());
    page.setNavigable(definition.navigable());
    page.setOrderIndex(definition.order());
    return pageRepository.save(page);
  }

  private PageAction createPageAction(WebPage page, Action action) {
    var pageAction = new PageAction();
    pageAction.setPage(page);
    pageAction.setAction(action);
    pageAction.setLabel(action.getName());
    return pageActionRepository.save(pageAction);
  }

  private void grant(Role role, PageAction pageAction) {
    boolean platformRole = TenantContext.PLATFORM.equals(role.getTenantCode());
    if (catalog.isPlatformOnly(pageAction.getPage().getCode()) && !platformRole) {
      return;
    }
    var permission = new Permission();
    permission.setTenantCode(role.getTenantCode());
    permission.setEntityType(EntityType.ROLE);
    permission.setEntityId(role.getId());
    permission.setPageAction(pageAction);
    permission.setGranted(true);
    permissionRepository.save(permission);
  }
}
