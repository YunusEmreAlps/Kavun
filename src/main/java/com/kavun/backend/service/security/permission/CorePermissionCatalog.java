package com.kavun.backend.service.security.permission;

import com.kavun.constant.PermissionConstants.Actions;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.enums.ActionType;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * The platform's own actions and administration pages.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class CorePermissionCatalog implements PermissionCatalogContributor {

  @Override
  public void contribute(PermissionCatalog.Builder catalog) {
    catalog
        .action(Actions.VIEW, "View", ActionType.PAGE)
        .action(Actions.CREATE, "Create", ActionType.BUTTON)
        .action(Actions.EDIT, "Edit", ActionType.BUTTON)
        .action(Actions.DELETE, "Delete", ActionType.BUTTON)
        .action(Actions.ASSIGN, "Assign", ActionType.BUTTON);

    catalog
        .page(Pages.ADMINISTRATION)
        .name("Administration")
        .url("/administration")
        .icon("fa-solid fa-gear")
        .notNavigable()
        .actions(Actions.VIEW);

    administrationPage(catalog, Pages.USERS, "Users", "fa-solid fa-users")
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
    administrationPage(catalog, Pages.ROLES, "Roles", "fa-solid fa-user-shield")
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE, Actions.ASSIGN);
    administrationPage(catalog, Pages.PERMISSIONS, "Permissions", "fa-solid fa-key")
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
    // Pages and actions are shared by every tenant, so only the platform may change them.
    administrationPage(catalog, Pages.PAGES, "Pages", "fa-solid fa-sitemap")
        .platformOnly()
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
    administrationPage(catalog, Pages.ACTIONS, "Actions", "fa-solid fa-bolt")
        .platformOnly()
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
    administrationPage(catalog, Pages.PAGE_ACTIONS, "Page Actions", "fa-solid fa-link")
        .platformOnly()
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
    administrationPage(catalog, Pages.USER_REPORTS, "User Reports", "fa-solid fa-chart-line")
        .actions(Actions.VIEW);
    administrationPage(catalog, Pages.TENANTS, "Tenants", "fa-solid fa-building")
        .platformOnly()
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
  }

  private static PermissionCatalog.PageSpec administrationPage(
      PermissionCatalog.Builder catalog, String code, String name, String icon) {
    return catalog
        .page(code)
        .parent(Pages.ADMINISTRATION)
        .name(name)
        .url("/administration/" + code.toLowerCase(Locale.ROOT).replace('_', '-'))
        .icon(icon);
  }
}
