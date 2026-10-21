package com.kavun.web.rest.v1;

import com.kavun.IntegrationTestUtils;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.domain.user.WebPage;
import com.kavun.backend.persistent.repository.ActionRepository;
import com.kavun.backend.persistent.repository.PageRepository;
import com.kavun.constant.PermissionConstants.Actions;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.UserDto;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** How page permissions shape the navigation tree returned to the frontend. */
class NavigationRestApiIntegrationTest extends IntegrationTestUtils {

  private static final String NAVIGATION = "$.data.navigation";

  @Autowired private transient PageRepository pageRepository;
  @Autowired private transient ActionRepository actionRepository;

  @Test
  void userWithoutPermissionsGetsEmptyNavigation() throws Exception {
    navigationOf(createUserWithRoles())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION).isEmpty());
  }

  @Test
  void childPermissionShowsItsGroupAsHeading() throws Exception {
    var user = createUserWithRoles(createRoleWithPermissions("USERS:VIEW", "USERS:EDIT"));

    navigationOf(user)
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + ".length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].code").value(Pages.ADMINISTRATION))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].access").value(false))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].actions").isEmpty())
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children.length()").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].code").value(Pages.USERS))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].access").value(true))
        .andExpect(
            MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].actions[0].code")
                .value(Actions.EDIT));
  }

  @Test
  void groupWithoutAccessibleChildrenIsHidden() throws Exception {
    var user = createUserWithRoles(createRoleWithPermissions("ADMINISTRATION:VIEW"));

    navigationOf(user).andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION).isEmpty());
  }

  @Test
  void screenWithoutViewPermissionIsHeadingForAccessibleChild() throws Exception {
    var details = createChildPage(Pages.USERS, "USER_DETAILS");
    var user = createUserWithRoles(createRoleWithPermissions(details.getCode() + ":VIEW"));

    navigationOf(user)
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].code").value(Pages.USERS))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].navigable").value(true))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].access").value(false))
        .andExpect(MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].actions").isEmpty())
        .andExpect(
            MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].children[0].code")
                .value(details.getCode()))
        .andExpect(
            MockMvcResultMatchers.jsonPath(NAVIGATION + "[0].children[0].children[0].access")
                .value(true));
  }

  private ResultActions navigationOf(UserDto user) throws Exception {
    return mockMvc.perform(
        MockMvcRequestBuilders.get("/api/v1/navigation")
            .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(user)));
  }

  private WebPage createChildPage(String parentCode, String code) {
    var page = new WebPage();
    page.setCode(code);
    page.setName(code);
    page.setUrl("/" + code.toLowerCase(Locale.ROOT));
    page.setIcon("fa-solid fa-file");
    page.setParent(pageRepository.findByCode(parentCode).orElseThrow());
    pageRepository.save(page);

    var view = new PageAction();
    view.setPage(page);
    view.setAction(actionRepository.findByCode(Actions.VIEW).orElseThrow());
    view.setLabel("View");
    pageActionRepository.save(view);
    return page;
  }
}
