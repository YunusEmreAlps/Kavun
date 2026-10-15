package com.kavun.config.security.authorization;

import com.kavun.annotation.PublicEndpoint;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.security.permission.PermissionCatalog;
import com.kavun.enums.ActionType;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class EndpointPolicyValidatorTest {

  // ORDERS declares only VIEW, so an endpoint requiring ORDERS:CREATE is invalid.
  private static final PermissionCatalog CATALOG = catalog();

  @Test
  void acceptsFullyDeclaredEndpoints() {
    Assertions.assertEquals(List.of(), validator(ValidController.class).validate());
  }

  @Test
  void reportsMissingPoliciesAndUnknownPermissions() {
    var violations = validator(InvalidController.class).validate();

    Assertions.assertEquals(2, violations.size(), violations.toString());
    Assertions.assertTrue(violations.stream().anyMatch(v -> v.contains("InvalidController#open")));
    Assertions.assertTrue(violations.stream().anyMatch(v -> v.contains("ORDERS:CREATE")));
  }

  @Test
  void ignoresNonApiEndpoints() {
    Assertions.assertEquals(List.of(), validator(WebPageController.class).validate());
  }

  @Test
  void failsStartupOnViolations() {
    var validator = validator(InvalidController.class);

    Assertions.assertThrows(EndpointPolicyException.class, validator::afterSingletonsInstantiated);
  }

  private static EndpointPolicyValidator validator(Class<?> controller) {
    var context = new GenericWebApplicationContext();
    context.registerBean(controller);
    context.refresh();

    var handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setApplicationContext(context);
    handlerMapping.afterPropertiesSet();

    return new EndpointPolicyValidator(handlerMapping, new EndpointPolicyResolver(), CATALOG);
  }

  private static PermissionCatalog catalog() {
    var builder =
        PermissionCatalog.builder()
            .action("VIEW", "View", ActionType.PAGE)
            .action("CREATE", "Create", ActionType.BUTTON);
    builder.page("ORDERS").name("Orders").actions("VIEW");
    return builder.build();
  }

  @RestController
  @RequestMapping("/api/v1/orders")
  @RequirePermission(page = "ORDERS")
  static class ValidController {
    @GetMapping
    public void list() {}

    @PublicEndpoint
    @PostMapping("/status")
    public void status() {}
  }

  @RestController
  @RequestMapping("/api/v1/orders")
  static class InvalidController {
    @GetMapping("/open")
    public void open() {}

    @RequirePermission(page = "ORDERS")
    @PostMapping
    public void create() {}
  }

  @RestController
  @RequestMapping("/pages")
  static class WebPageController {
    @GetMapping
    public void index() {}
  }
}
