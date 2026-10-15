package com.kavun.config.security;

import com.kavun.IntegrationTestUtils;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * End-to-end checks of endpoint authorization: server-bound page permissions, the guards against
 * privilege escalation and the rejection of tokens whose user or session is no longer valid.
 */
@TestInstance(Lifecycle.PER_CLASS)
class AuthorizationIntegrationTest extends IntegrationTestUtils {

  private static final String ROLES_URL = "/api/v1/role";
  private static final String USERS_URL = "/api/v1/users";
  private static final String NAVIGATION_URL = "/api/v1/navigation";

  private transient UserDto admin;
  private transient Long adminRoleId;

  @BeforeAll
  void beforeAll() {
    admin = createAndAssertAdmin(UserUtils.createUserDto(true));
    adminRoleId = roleService.findByName(RoleType.ROLE_ADMIN.getName()).getId();
  }

  @Test
  void anonymousCallerIsUnauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ROLES_URL))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void userWithoutPermissionIsForbidden() throws Exception {
    var user = createUserWithRoles();

    perform(MockMvcRequestBuilders.get(ROLES_URL), user)
        .andExpect(MockMvcResultMatchers.status().isForbidden())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(403))
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message")
                .value("You do not have permission to perform this action."));
  }

  @Test
  void forbiddenMessageFollowsAcceptLanguage() throws Exception {
    var user = createUserWithRoles();

    perform(MockMvcRequestBuilders.get(ROLES_URL).header(HttpHeaders.ACCEPT_LANGUAGE, "tr-TR"), user)
        .andExpect(MockMvcResultMatchers.status().isForbidden())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.message")
                .value("Bu işlemi gerçekleştirmek için izniniz bulunmamaktadır."));
  }

  @Test
  void grantedPermissionAllowsOnlyThatAction() throws Exception {
    var user = createUserWithRoles(createRoleWithPermissions("ROLES:VIEW"));

    perform(MockMvcRequestBuilders.get(ROLES_URL), user)
        .andExpect(MockMvcResultMatchers.status().isOk());
    perform(
            MockMvcRequestBuilders.post(ROLES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ROLE_SHOULD_NOT_EXIST\"}"),
            user)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    Assertions.assertNull(roleService.findByName("ROLE_SHOULD_NOT_EXIST"));
  }

  @Test
  void pageCodeHeaderDoesNotGrantAccess() throws Exception {
    var user = createUserWithRoles(createRoleWithPermissions("ROLES:VIEW", "ROLES:DELETE"));
    var victim = createUserWithRoles();

    // Holding DELETE on ROLES must not allow deleting users by claiming to be on the ROLES page.
    perform(
            MockMvcRequestBuilders.delete(USERS_URL + "/" + victim.getId())
                .header("Page-Code", Pages.ROLES),
            user)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    Assertions.assertNotNull(userService.findByUsername(victim.getUsername()));
  }

  @Test
  void userCannotGrantThemselvesAdmin() throws Exception {
    var user = createUserWithRoles();

    perform(assignRoles(user, adminRoleId), user)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    Assertions.assertNotEquals(
        RoleType.ROLE_ADMIN.getName(), userService.findById(user.getId()).getRole());
  }

  @Test
  void roleAssignerCanHandOutRolesButNotAdmin() throws Exception {
    var assigner = createUserWithRoles(createRoleWithPermissions("ROLES:ASSIGN"));
    var assignableRole = createRoleWithPermissions();

    perform(assignRoles(assigner, adminRoleId), assigner)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    perform(assignRoles(assigner, assignableRole.getId()), assigner)
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void userManagerCannotChangeAdminAccounts() throws Exception {
    var manager = createUserWithRoles(createRoleWithPermissions("USERS:EDIT"));
    var regularUser = createUserWithRoles();

    perform(MockMvcRequestBuilders.put(USERS_URL + "/" + admin.getId() + "/disable"), manager)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    Assertions.assertTrue(userService.getUserDetails(admin.getUsername()).isEnabled());

    perform(MockMvcRequestBuilders.put(USERS_URL + "/" + regularUser.getId() + "/disable"), manager)
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void adminGetsEveryCatalogPermission() throws Exception {
    perform(MockMvcRequestBuilders.get("/api/v1/permission"), admin)
        .andExpect(MockMvcResultMatchers.status().isOk());
    perform(MockMvcRequestBuilders.get(NAVIGATION_URL), admin)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.navigation[0].code").value(Pages.ADMINISTRATION));
  }

  @Test
  void tokenOfLoggedOutSessionIsRejected() throws Exception {
    var user = createUserWithRoles();
    var token = bearerTokenFor(user);

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(MockMvcResultMatchers.status().isOk());

    mockMvc
        .perform(MockMvcRequestBuilders.get(NAVIGATION_URL).header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void tokenOfDisabledUserIsRejected() throws Exception {
    var user = createUserWithRoles();
    var token = bearerTokenFor(user);

    userService.disableUser(user.getId());

    mockMvc
        .perform(MockMvcRequestBuilders.get(NAVIGATION_URL).header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void requestBodyIsNotReadBeforeAuthorization() throws Exception {
    // An anonymous caller sending an invalid body learns nothing about validation: it gets 401.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ROLES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void unknownApiRoutesAreNotFound() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/does-not-exist"))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  private MockHttpServletRequestBuilder assignRoles(UserDto user, Long roleId) {
    return MockMvcRequestBuilders.post(ROLES_URL + "/assign-roles/" + user.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(List.of(roleId)));
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, UserDto user)
      throws Exception {
    return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerTokenFor(user)));
  }
}
