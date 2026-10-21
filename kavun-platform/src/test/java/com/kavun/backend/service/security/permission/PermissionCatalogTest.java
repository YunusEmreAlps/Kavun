package com.kavun.backend.service.security.permission;

import com.kavun.backend.service.security.permission.PermissionCatalog.PageDefinition;
import com.kavun.enums.ActionType;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

  @Test
  void ordersParentsBeforeChildrenAndAppliesDefaults() {
    var builder = withActions();
    builder.page("PAGE_ACTIONS").parent("ADMINISTRATION").name("Page Actions").actions("VIEW");
    builder.page("ADMINISTRATION").name("Administration").actions("VIEW");

    var catalog = builder.build();

    Assertions.assertEquals(
        List.of("ADMINISTRATION", "PAGE_ACTIONS"),
        catalog.getPages().stream().map(PageDefinition::code).toList());
    var pageActions = catalog.getPages().stream().toList().get(1);
    Assertions.assertEquals("/page-actions", pageActions.url());
    Assertions.assertTrue(pageActions.showInMenu());
    Assertions.assertTrue(catalog.contains(new PageActionCode("PAGE_ACTIONS", "VIEW")));
    Assertions.assertFalse(catalog.contains(new PageActionCode("PAGE_ACTIONS", "EDIT")));
    Assertions.assertFalse(catalog.contains(new PageActionCode("UNKNOWN", "VIEW")));
  }

  @Test
  void rejectsUndeclaredAction() {
    var builder = withActions();
    builder.page("USERS").name("Users").actions("APPROVE");

    Assertions.assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void rejectsUndeclaredParent() {
    var builder = withActions();
    builder.page("USERS").name("Users").parent("SETTINGS").actions("VIEW");

    Assertions.assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void rejectsPageWithoutActions() {
    var builder = withActions();
    builder.page("USERS").name("Users");

    Assertions.assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void rejectsDuplicates() {
    var builder = withActions();
    builder.page("USERS").name("Users").actions("VIEW");

    Assertions.assertThrows(IllegalStateException.class, () -> builder.page("USERS"));
    Assertions.assertThrows(
        IllegalStateException.class, () -> builder.action("VIEW", "View", ActionType.PAGE));
  }

  @Test
  void rejectsParentCycle() {
    var builder = withActions();
    builder.page("A").name("A").parent("B").actions("VIEW");
    builder.page("B").name("B").parent("A").actions("VIEW");

    Assertions.assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  void parsesPageActionCodes() {
    Assertions.assertEquals(new PageActionCode("USERS", "EDIT"), PageActionCode.parse("USERS:EDIT"));
    Assertions.assertEquals("USERS:EDIT", new PageActionCode("USERS", "EDIT").toString());
    Assertions.assertThrows(IllegalArgumentException.class, () -> PageActionCode.parse("USERS"));
    Assertions.assertThrows(IllegalArgumentException.class, () -> PageActionCode.parse("USERS:"));
  }

  private static PermissionCatalog.Builder withActions() {
    return PermissionCatalog.builder()
        .action("VIEW", "View", ActionType.PAGE)
        .action("EDIT", "Edit", ActionType.BUTTON);
  }
}
