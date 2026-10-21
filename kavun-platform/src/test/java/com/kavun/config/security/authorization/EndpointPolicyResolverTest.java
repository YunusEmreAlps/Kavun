package com.kavun.config.security.authorization;

import com.kavun.annotation.AuthenticatedEndpoint;
import com.kavun.annotation.PublicEndpoint;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.security.permission.PageActionCode;
import com.kavun.config.security.authorization.EndpointPolicy.Access;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.method.HandlerMethod;

class EndpointPolicyResolverTest {

  private final EndpointPolicyResolver resolver = new EndpointPolicyResolver();

  @ParameterizedTest
  @CsvSource({"GET,VIEW", "HEAD,VIEW", "POST,CREATE", "PUT,EDIT", "PATCH,EDIT", "DELETE,DELETE"})
  void derivesActionFromHttpMethod(String httpMethod, String action) throws Exception {
    var policy = resolve(OrdersController.class, "inherited", httpMethod);

    Assertions.assertEquals(Access.PERMISSION, policy.access());
    Assertions.assertEquals(new PageActionCode("ORDERS", action), policy.permission());
  }

  @Test
  void methodActionOverridesHttpMethod() throws Exception {
    var policy = resolve(OrdersController.class, "search", "POST");

    Assertions.assertEquals(new PageActionCode("ORDERS", "VIEW"), policy.permission());
  }

  @Test
  void methodPageOverridesControllerPage() throws Exception {
    var policy = resolve(OrdersController.class, "invoices", "GET");

    Assertions.assertEquals(new PageActionCode("INVOICES", "VIEW"), policy.permission());
  }

  @Test
  void methodPolicyReplacesControllerPolicy() throws Exception {
    Assertions.assertEquals(EndpointPolicy.PUBLIC, resolve(OrdersController.class, "status", "GET"));
    Assertions.assertEquals(
        EndpointPolicy.AUTHENTICATED, resolve(OrdersController.class, "mine", "GET"));
  }

  @Test
  void controllerWideAuthenticatedPolicy() throws Exception {
    Assertions.assertEquals(
        EndpointPolicy.AUTHENTICATED, resolve(ProfileController.class, "show", "GET"));
  }

  @Test
  void methodPermissionOnAuthenticatedControllerNeedsItsOwnPage() throws Exception {
    var policy = resolve(ProfileController.class, "audit", "GET");

    Assertions.assertEquals(new PageActionCode("AUDIT", "VIEW"), policy.permission());
  }

  @Test
  void missingPolicyIsRejected() {
    var exception =
        Assertions.assertThrows(
            EndpointPolicyException.class, () -> resolve(UnprotectedController.class, "list", "GET"));
    Assertions.assertTrue(exception.getMessage().contains("UnprotectedController#list"));
  }

  @Test
  void contradictoryPoliciesAreRejected() {
    Assertions.assertThrows(
        EndpointPolicyException.class, () -> resolve(UnprotectedController.class, "both", "GET"));
  }

  @Test
  void permissionWithoutPageIsRejected() {
    Assertions.assertThrows(
        EndpointPolicyException.class, () -> resolve(UnprotectedController.class, "noPage", "GET"));
  }

  @Test
  void controllerLevelActionIsRejected() {
    Assertions.assertThrows(
        EndpointPolicyException.class, () -> resolve(ReportsController.class, "list", "GET"));
  }

  @Test
  void unmappedHttpMethodNeedsExplicitAction() {
    Assertions.assertThrows(
        EndpointPolicyException.class, () -> resolve(OrdersController.class, "inherited", "OPTIONS"));
  }

  private EndpointPolicy resolve(Class<?> controller, String method, String httpMethod)
      throws Exception {
    var handlerMethod =
        new HandlerMethod(
            controller.getDeclaredConstructor().newInstance(), controller.getMethod(method));
    return resolver.resolve(handlerMethod, httpMethod);
  }

  @RequirePermission(page = "ORDERS")
  static class OrdersController {
    public void inherited() {}

    @RequirePermission(action = "VIEW")
    public void search() {}

    @RequirePermission(page = "INVOICES")
    public void invoices() {}

    @PublicEndpoint
    public void status() {}

    @AuthenticatedEndpoint
    public void mine() {}
  }

  @AuthenticatedEndpoint
  static class ProfileController {
    public void show() {}

    @RequirePermission(page = "AUDIT")
    public void audit() {}
  }

  @RequirePermission(page = "REPORTS", action = "VIEW")
  static class ReportsController {
    public void list() {}
  }

  static class UnprotectedController {
    public void list() {}

    @PublicEndpoint
    @AuthenticatedEndpoint
    public void both() {}

    @RequirePermission
    public void noPage() {}
  }
}
