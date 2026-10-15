package com.kavun.web.rest.v1;

import com.kavun.IntegrationTestUtils;
import com.kavun.constant.AdminConstants;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@TestInstance(Lifecycle.PER_CLASS)
class UserRestApiIntegrationTest extends IntegrationTestUtils {

  private transient String adminBearerToken;

  @BeforeAll
  void beforeAll() {
    var admin = createAndAssertAdmin(UserUtils.createUserDto(true));
    adminBearerToken = bearerTokenFor(admin.getUsername());
  }

  @Test
  void getUserWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(userUrl(1L)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void enableUserWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.put(userUrl(1L) + "/enable"))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void deleteUserWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.delete(userUrl(1L)))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void getUserById(TestInfo testInfo) throws Exception {
    UserDto userDto = createAndAssertUser("rest-" + testInfo.getDisplayName(), true);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(userUrl(userDto.getId()))
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value(userDto.getUsername()));
  }

  @Test
  void enableAndDisableUser(TestInfo testInfo) throws Exception {
    UserDto userDto = createAndAssertUser("rest-" + testInfo.getDisplayName(), false);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put(userUrl(userDto.getId()) + "/enable")
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
        .andExpect(MockMvcResultMatchers.status().isOk());
    Assertions.assertTrue(userService.getUserDetails(userDto.getUsername()).isEnabled());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put(userUrl(userDto.getId()) + "/disable")
                .header(HttpHeaders.AUTHORIZATION, adminBearerToken))
        .andExpect(MockMvcResultMatchers.status().isOk());
    Assertions.assertFalse(userService.getUserDetails(userDto.getUsername()).isEnabled());
  }

  private static String userUrl(Long id) {
    return String.format("%s/%d", AdminConstants.API_V1_USERS_ROOT_URL, id);
  }
}
