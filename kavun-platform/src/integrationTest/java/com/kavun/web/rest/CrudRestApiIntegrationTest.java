package com.kavun.web.rest;

import com.jayway.jsonpath.JsonPath;
import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.backend.service.user.ActionService;
import com.kavun.backend.service.user.PageService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.RoleDto;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.request.RoleRequest;
import com.kavun.shared.util.UserUtils;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** The generic CRUD endpoints and the list filters every service shares. */
@TestInstance(Lifecycle.PER_CLASS)
class CrudRestApiIntegrationTest extends IntegrationTestUtils {

  private static final String ROLES_URL = "/api/v1/role";

  @Autowired private transient PageService pageService;
  @Autowired private transient ActionService actionService;
  @Autowired private transient PageRepository pageRepository;

  private transient UserDto admin;

  @BeforeAll
  void beforeAll() {
    admin = createAndAssertAdmin(UserUtils.createUserDto(true));
  }

  @Test
  void crudLifecycleThroughGenericEndpoints() throws Exception {
    String name = uniqueRoleName();
    var created =
        perform(
                MockMvcRequestBuilders.post(ROLES_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\",\"label\":\"Temporary\"}"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value(name))
            .andReturn();
    Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
    String roleUrl = ROLES_URL + "/" + id;

    perform(MockMvcRequestBuilders.get(roleUrl))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.label").value("Temporary"));
    perform(
            MockMvcRequestBuilders.put(roleUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"label\":\"Renamed\"}"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.label").value("Renamed"));
    perform(MockMvcRequestBuilders.get(ROLES_URL + "/count").param("name", name))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));
    perform(
            MockMvcRequestBuilders.post(ROLES_URL + "/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"renamed\",\"name\":\"" + name + "\"}"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(1));

    perform(MockMvcRequestBuilders.delete(roleUrl))
        .andExpect(MockMvcResultMatchers.status().isNoContent());
    perform(MockMvcRequestBuilders.get(ROLES_URL + "/count").param("name", name))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(0));
    perform(
            MockMvcRequestBuilders.get(ROLES_URL + "/count")
                .param("name", name)
                .param("deleted", "true"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(1));

    perform(MockMvcRequestBuilders.post(roleUrl + "/restore"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.deleted").value(false));
    perform(MockMvcRequestBuilders.get(roleUrl + "/exists"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true));
  }

  @Test
  void filterValuesAreLowercasedRegardlessOfServerLocale() {
    var role = createRole("kuyumcu izmir subesi");
    var defaultLocale = Locale.getDefault();
    try {
      // With a Turkish default locale "IZMIR".toLowerCase() is "ızmır" (dotless i) and would
      // never match; the filter must lowercase locale-independently.
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      Assertions.assertTrue(containsRole(Map.of("description", "IZMIR"), role));
      Assertions.assertTrue(containsRole(Map.of("description", "Izmir Subesi"), role));
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  void likeWildcardsInFilterValuesAreLiteral() {
    var discounted = createRole("Campaign 50% off");
    var other = createRole("Campaign 500 off");

    Assertions.assertTrue(containsRole(Map.of("description", "50%"), discounted));
    Assertions.assertFalse(containsRole(Map.of("description", "50%"), other));
  }

  @Test
  void filtersByBooleanEnumAndRelation() {
    var administration = pageRepository.findByCode(Pages.ADMINISTRATION).orElseThrow();

    var children = pageService.findAll(Map.of("parentId", administration.getId().toString()));
    Assertions.assertTrue(children.stream().anyMatch(page -> Pages.USERS.equals(page.getCode())));
    Assertions.assertTrue(children.stream().noneMatch(page -> Pages.ADMINISTRATION.equals(page.getCode())));

    var groups = pageService.findAll(Map.of("navigable", "false"));
    Assertions.assertTrue(groups.stream().anyMatch(page -> Pages.ADMINISTRATION.equals(page.getCode())));
    Assertions.assertTrue(groups.stream().noneMatch(page -> Pages.USERS.equals(page.getCode())));

    var buttons = actionService.findAll(Map.of("type", "button"));
    Assertions.assertFalse(buttons.isEmpty());
    Assertions.assertTrue(buttons.stream().noneMatch(action -> "VIEW".equals(action.getCode())));
  }

  @Test
  void filtersDateTimeFieldsByDayAndRange() {
    var role = createRole("Created today");
    String today = LocalDate.now().toString();
    String tomorrow = LocalDate.now().plusDays(1).toString();

    Assertions.assertTrue(containsRole(Map.of("createdAt", today), role));
    Assertions.assertTrue(containsRole(Map.of("createdAtFrom", today, "createdAtTo", today), role));
    Assertions.assertFalse(containsRole(Map.of("createdAtFrom", tomorrow), role));
  }

  @Test
  void fieldsOutsideTheAllowListAreIgnored() {
    // A password hash must never become a search oracle: the filter is dropped, not applied.
    var users = userService.findAll(Map.of("password", "no-hash-looks-like-this"), Pageable.unpaged());

    Assertions.assertFalse(users.isEmpty());
  }

  @Test
  void invalidFilterValueIsBadRequest() throws Exception {
    perform(MockMvcRequestBuilders.get("/api/v1/page").param("navigable", "maybe"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400));
  }

  private RoleDto createRole(String description) {
    return roleService.create(
        RoleRequest.builder().name(uniqueRoleName()).description(description).build());
  }

  private boolean containsRole(Map<String, String> filters, RoleDto role) {
    return roleService.findAll(filters).stream().anyMatch(found -> found.getId().equals(role.getId()));
  }

  private static String uniqueRoleName() {
    return "ROLE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
  }

  private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerTokenFor(admin)));
  }
}
