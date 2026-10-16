package com.kavun.web.rest.v1;

import com.jayway.jsonpath.JsonPath;
import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.repository.ApplicationLogRepository;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** Tenants are provisioned through the API and cannot see or touch each other's data. */
@TestInstance(Lifecycle.PER_CLASS)
class TenantIsolationIntegrationTest extends IntegrationTestUtils {

  private static final String TENANT_URL = "/api/v1/tenant";

  @Autowired private transient ApplicationLogRepository applicationLogRepository;

  private transient UserDto platformAdmin;
  private transient Tenant acme;
  private transient Tenant globex;

  private record Tenant(Long id, String code, UserDto admin) {}

  @BeforeAll
  void beforeAll() throws Exception {
    platformAdmin = createAndAssertAdmin(UserUtils.createUserDto(true));
    acme = provisionTenant();
    globex = provisionTenant();
  }

  @Test
  void provisionedTenantHasItsOwnRolesAndAdmin() throws Exception {
    perform(MockMvcRequestBuilders.get("/api/v1/role/list"), acme.admin())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data[*].name").value(
            org.hamcrest.Matchers.containsInAnyOrder(
                RoleType.ROLE_ADMIN.getName(), RoleType.ROLE_USER.getName())));
    Assertions.assertEquals(acme.code(), acme.admin().getTenantCode());
  }

  @Test
  void usersOfOtherTenantsAreInvisible() throws Exception {
    UserDto acmeUser =
        TenantContext.callAs(acme.code(), () -> createAndAssertUser(UserUtils.createUserDto(true)));
    String userUrl = "/api/v1/users/" + acmeUser.getId();

    perform(MockMvcRequestBuilders.get(userUrl), acme.admin())
        .andExpect(MockMvcResultMatchers.status().isOk());
    // Asked twice on purpose: nothing cached for one tenant may be served to another.
    perform(MockMvcRequestBuilders.get(userUrl), globex.admin())
        .andExpect(MockMvcResultMatchers.status().isNotFound());
    perform(
            MockMvcRequestBuilders.post("/api/v1/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(Map.of("username", acmeUser.getUsername()))),
            globex.admin())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isEmpty());
  }

  @Test
  void rolesOfOtherTenantsCannotBeAssigned() throws Exception {
    Long acmeAdminRoleId =
        TenantContext.callAs(acme.code(), () -> roleService.findByName(RoleType.ROLE_ADMIN.getName()))
            .getId();
    UserDto globexUser =
        TenantContext.callAs(globex.code(), () -> createAndAssertUser(UserUtils.createUserDto(true)));

    perform(
            MockMvcRequestBuilders.post("/api/v1/role/assign-roles/" + globexUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(List.of(acmeAdminRoleId))),
            globex.admin())
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  void platformPagesAreClosedToTenantAdmins() throws Exception {
    perform(MockMvcRequestBuilders.get(TENANT_URL), acme.admin())
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    perform(MockMvcRequestBuilders.get("/api/v1/page"), acme.admin())
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    String navigation =
        perform(MockMvcRequestBuilders.get("/api/v1/navigation"), acme.admin())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Assertions.assertTrue(navigation.contains(Pages.USERS));
    Assertions.assertFalse(navigation.contains(Pages.TENANTS));
    Assertions.assertFalse(navigation.contains(Pages.PAGE_ACTIONS));
  }

  @Test
  void platformPagesCannotBeGrantedInsideTenants() throws Exception {
    Long acmeAdminRoleId =
        TenantContext.callAs(acme.code(), () -> roleService.findByName(RoleType.ROLE_ADMIN.getName()))
            .getId();
    Long tenantsViewId =
        pageActionRepository.findActionByPageCodeAndActionCode(Pages.TENANTS, "VIEW").orElseThrow().getId();

    perform(
            MockMvcRequestBuilders.post("/api/v1/permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    asJsonString(
                        Map.of(
                            "entityType", "ROLE",
                            "entityId", acmeAdminRoleId,
                            "pageActionId", tenantsViewId,
                            "granted", true))),
            acme.admin())
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void disablingATenantLocksItsUsersOut() throws Exception {
    Tenant initech = provisionTenant();
    String token = bearerTokenFor(initech.admin());

    perform(
            MockMvcRequestBuilders.put(TENANT_URL + "/" + initech.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Initech\",\"enabled\":false}"),
            platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.enabled").value(false));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/navigation").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void platformTenantCannotBeDisabledOrDeleted() throws Exception {
    Long platformId =
        tenantId(
            perform(
                    MockMvcRequestBuilders.get(TENANT_URL).param("code", TenantContext.PLATFORM),
                    platformAdmin)
                .andReturn()
                .getResponse()
                .getContentAsString());

    perform(
            MockMvcRequestBuilders.put(TENANT_URL + "/" + platformId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Platform\",\"enabled\":false}"),
            platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isConflict());
    perform(MockMvcRequestBuilders.delete(TENANT_URL + "/" + platformId), platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isConflict());
  }

  @Test
  void activityLogsBelongToTheTenantOfTheRequest() throws Exception {
    perform(MockMvcRequestBuilders.get("/api/v1/navigation"), globex.admin())
        .andExpect(MockMvcResultMatchers.status().isOk());

    // Logs are written asynchronously.
    long globexLogs = 0;
    for (int attempt = 0; attempt < 50 && globexLogs == 0; attempt++) {
      Thread.sleep(100);
      globexLogs = TenantContext.callAs(globex.code(), applicationLogRepository::count);
    }
    Assertions.assertTrue(globexLogs > 0);

    String response =
        perform(MockMvcRequestBuilders.get("/api/v1/users/reports/activity-logs"), globex.admin())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer visible = JsonPath.read(response, "$.data.page.totalElements");
    long everyTenant = TenantContext.callAs(TenantContext.ROOT, applicationLogRepository::count);
    Assertions.assertTrue(visible > 0 && visible < everyTenant);
  }

  @Test
  void invalidTenantRequestsAreRejected() throws Exception {
    String code = uniqueCode();
    perform(createTenant(code.toUpperCase(Locale.ROOT), "admin-" + code, code + "@example.com"), platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
    perform(
            MockMvcRequestBuilders.post(TENANT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"name\":\"No admin\"}"),
            platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
    // The administrator's email is already taken by the tenant provisioned in beforeAll.
    perform(createTenant(code, "admin-" + code, acme.admin().getEmail()), platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isConflict());
    perform(createTenant(acme.code(), "admin-" + code, code + "@example.com"), platformAdmin)
        .andExpect(MockMvcResultMatchers.status().isConflict());
  }

  private Tenant provisionTenant() throws Exception {
    String code = uniqueCode();
    String adminUsername = "admin-" + code;
    String response =
        perform(createTenant(code, adminUsername, adminUsername + "@example.com"), platformAdmin)
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.code").value(code))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(response, "$.data.id");
    UserDto admin = TenantContext.callAs(code, () -> userService.findByUsername(adminUsername));
    return new Tenant(id.longValue(), code, admin);
  }

  private static MockHttpServletRequestBuilder createTenant(
      String code, String adminUsername, String adminEmail) {
    return MockMvcRequestBuilders.post(TENANT_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            asJsonString(
                Map.of(
                    "code", code,
                    "name", "Tenant " + code,
                    "adminUsername", adminUsername,
                    "adminEmail", adminEmail,
                    "adminPassword", "s3cret-password")));
  }

  private static Long tenantId(String listResponse) {
    Integer id = JsonPath.read(listResponse, "$.data.content[0].id");
    return id.longValue();
  }

  private static String uniqueCode() {
    return "t" + UUID.randomUUID().toString().substring(0, 8);
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, UserDto user)
      throws Exception {
    return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerTokenFor(user)));
  }
}
