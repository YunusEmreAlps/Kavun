package com.kavun.web.rest.v1;

import com.jayway.jsonpath.JsonPath;
import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.domain.user.Permission;
import com.kavun.backend.persistent.domain.user.Role;
import com.kavun.constant.PermissionConstants.Actions;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.enums.EntityType;
import com.kavun.enums.RoleType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/** The permission matrix of a role: reading it, saving it, and who may change what. */
@TestInstance(Lifecycle.PER_CLASS)
class PermissionMatrixIntegrationTest extends IntegrationTestUtils {

  private static final String MATRIX_URL = "/api/v1/permission/role/";

  private transient UserDto admin;

  @BeforeAll
  void beforeAll() {
    admin = createAndAssertAdmin(UserUtils.createUserDto(true));
  }

  @Test
  void matrixListsThePageActionsWithTheRolesGrants() throws Exception {
    Role role = createRoleWithPermissions("USERS:VIEW");

    String matrix = getMatrix(role.getId(), admin);

    Assertions.assertEquals(role.getName(), JsonPath.read(matrix, "$.data.role.name"));
    Assertions.assertEquals(Boolean.TRUE, JsonPath.read(matrix, "$.data.editable"));
    Assertions.assertEquals(
        List.of(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE, Actions.ASSIGN),
        JsonPath.read(matrix, "$.data.actions[*].code"));
    Assertions.assertEquals("GRANTED", state(matrix, Pages.USERS, Actions.VIEW));
    Assertions.assertEquals("NONE", state(matrix, Pages.USERS, Actions.EDIT));
    // Administration groups the other pages and comes first.
    Assertions.assertEquals(Pages.ADMINISTRATION, JsonPath.read(matrix, "$.data.pages[0].code"));
    Assertions.assertEquals(Integer.valueOf(1), level(matrix, Pages.USERS));
  }

  @Test
  void savingGrantsCheckedCellsAndRevokesUncheckedOnes() throws Exception {
    Role role = createRoleWithPermissions("USERS:VIEW");
    Long usersView = pageActionId(Pages.USERS, Actions.VIEW);
    Long usersEdit = pageActionId(Pages.USERS, Actions.EDIT);

    String matrix = saveMatrix(role.getId(), Set.of(usersEdit), admin);
    Assertions.assertEquals("NONE", state(matrix, Pages.USERS, Actions.VIEW));
    Assertions.assertEquals("GRANTED", state(matrix, Pages.USERS, Actions.EDIT));

    // Granting a revoked page action again reuses its soft-deleted record (unique per role).
    matrix = saveMatrix(role.getId(), Set.of(usersView, usersEdit), admin);
    Assertions.assertEquals("GRANTED", state(matrix, Pages.USERS, Actions.VIEW));
    List<Permission> records =
        permissionRepository.findAllByEntityTypeAndEntityId(EntityType.ROLE, role.getId());
    Assertions.assertEquals(2, records.size());
    Assertions.assertTrue(records.stream().noneMatch(Permission::isDeleted));
  }

  @Test
  void denialsStayUntilTheirCellIsChecked() throws Exception {
    Role role = createRoleWithPermissions();
    var denial = new Permission();
    denial.setEntityType(EntityType.ROLE);
    denial.setEntityId(role.getId());
    denial.setPageAction(
        pageActionRepository.findActionByPageCodeAndActionCode(Pages.USERS, Actions.DELETE).orElseThrow());
    denial.setGranted(false);
    permissionRepository.save(denial);

    String matrix = saveMatrix(role.getId(), Set.of(), admin);
    Assertions.assertEquals("DENIED", state(matrix, Pages.USERS, Actions.DELETE));

    matrix = saveMatrix(role.getId(), Set.of(pageActionId(Pages.USERS, Actions.DELETE)), admin);
    Assertions.assertEquals("GRANTED", state(matrix, Pages.USERS, Actions.DELETE));
  }

  @Test
  void adminRoleIsReadOnly() throws Exception {
    Long adminRoleId = roleService.findByName(RoleType.ROLE_ADMIN.getName()).getId();

    Assertions.assertEquals(Boolean.FALSE, JsonPath.read(getMatrix(adminRoleId, admin), "$.data.editable"));
    perform(putMatrix(adminRoleId, Set.of()), admin)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void permissionManagersOnlyGrantWhatTheyHold() throws Exception {
    UserDto manager =
        createUserWithRoles(createRoleWithPermissions("PERMISSIONS:VIEW", "PERMISSIONS:EDIT", "USERS:VIEW"));
    Role role = createRoleWithPermissions();
    Long usersView = pageActionId(Pages.USERS, Actions.VIEW);
    Long usersDelete = pageActionId(Pages.USERS, Actions.DELETE);

    String matrix = getMatrix(role.getId(), manager);
    Assertions.assertEquals(List.of(true), JsonPath.read(matrix, cellPath(Pages.USERS, Actions.VIEW, "grantable")));
    Assertions.assertEquals(List.of(false), JsonPath.read(matrix, cellPath(Pages.USERS, Actions.DELETE, "grantable")));

    perform(putMatrix(role.getId(), Set.of(usersView)), manager)
        .andExpect(MockMvcResultMatchers.status().isOk());
    perform(putMatrix(role.getId(), Set.of(usersView, usersDelete)), manager)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
    // Nor through the plain permission API.
    perform(
            MockMvcRequestBuilders.post("/api/v1/permission")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(Map.of(
                    "entityType", "ROLE", "entityId", role.getId(), "pageActionId", usersDelete, "granted", true))),
            manager)
        .andExpect(MockMvcResultMatchers.status().isForbidden());

    // Built-in roles are only changed by administrators.
    Long userRoleId = roleService.findByName(RoleType.ROLE_USER.getName()).getId();
    Assertions.assertEquals(Boolean.FALSE, JsonPath.read(getMatrix(userRoleId, manager), "$.data.editable"));
    perform(putMatrix(userRoleId, Set.of()), manager)
        .andExpect(MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  void unknownRolesAndPageActionsAreRejected() throws Exception {
    perform(MockMvcRequestBuilders.get(MATRIX_URL + Long.MAX_VALUE), admin)
        .andExpect(MockMvcResultMatchers.status().isNotFound());
    // An IllegalArgumentException, reported as 409 (docs/API_RESPONSE_STANDARDS.md).
    perform(putMatrix(createRoleWithPermissions().getId(), Set.of(Long.MAX_VALUE)), admin)
        .andExpect(MockMvcResultMatchers.status().isConflict());
  }

  private String getMatrix(Long roleId, UserDto user) throws Exception {
    return perform(MockMvcRequestBuilders.get(MATRIX_URL + roleId), user)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String saveMatrix(Long roleId, Set<Long> pageActionIds, UserDto user) throws Exception {
    return perform(putMatrix(roleId, pageActionIds), user)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static MockHttpServletRequestBuilder putMatrix(Long roleId, Set<Long> pageActionIds) {
    return MockMvcRequestBuilders.put(MATRIX_URL + roleId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(Map.of("pageActionIds", pageActionIds)));
  }

  private ResultActions perform(MockHttpServletRequestBuilder request, UserDto user) throws Exception {
    return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerTokenFor(user)));
  }

  private Long pageActionId(String page, String action) {
    return pageActionRepository.findActionByPageCodeAndActionCode(page, action).orElseThrow().getId();
  }

  private static String state(String matrix, String page, String action) {
    List<String> states = JsonPath.read(matrix, cellPath(page, action, "state"));
    return states.getFirst();
  }

  private static Integer level(String matrix, String page) {
    List<Integer> levels = JsonPath.read(matrix, "$.data.pages[?(@.code == '" + page + "')].level");
    return levels.getFirst();
  }

  private static String cellPath(String page, String action, String field) {
    return "$.data.pages[?(@.code == '" + page + "')].cells[?(@.actionCode == '" + action + "')]." + field;
  }
}
