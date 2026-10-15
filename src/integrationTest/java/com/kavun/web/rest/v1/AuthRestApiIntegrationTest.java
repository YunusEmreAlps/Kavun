package com.kavun.web.rest.v1;

import com.kavun.IntegrationTestUtils;
import com.kavun.TestUtils;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.TokenType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.JwtUtils;
import com.kavun.shared.util.core.JwtUtils.JwtTokenType;
import com.kavun.web.payload.request.LoginRequest;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@TestInstance(Lifecycle.PER_CLASS)
class AuthRestApiIntegrationTest extends IntegrationTestUtils {

  private static final String ACCESS_TOKEN_PATH = "$.data.access_token";

  private transient String loginUri;
  private transient String logoutUri;
  private transient String refreshUri;
  private transient String loginRequestJson;
  private transient UserDto storedUser;

  @BeforeAll
  void beforeAll() {
    var userDto = UserUtils.createUserDto(true);
    storedUser = createAndAssertAdmin(userDto);

    var loginRequest = new LoginRequest(storedUser.getUsername(), userDto.getPassword(), null, null);
    loginRequestJson = TestUtils.toJson(loginRequest);

    String delimiter = "/";
    loginUri =
        String.join(delimiter, SecurityConstants.API_V1_AUTH_ROOT_URL, SecurityConstants.LOGIN);
    logoutUri =
        String.join(delimiter, SecurityConstants.API_V1_AUTH_ROOT_URL, SecurityConstants.LOGOUT);
    refreshUri =
        String.join(
            delimiter, SecurityConstants.API_V1_AUTH_ROOT_URL, SecurityConstants.REFRESH_TOKEN);
  }

  @Test
  void loginWithValidCredentialsReturnsAccessTokenAndRefreshCookie() throws Exception {
    var refreshTokenDuration = Duration.ofDays(SecurityConstants.DEFAULT_TOKEN_DURATION);

    MvcResult mvcResult =
        performRequest(MockMvcRequestBuilders.post(loginUri), loginRequestJson)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath(ACCESS_TOKEN_PATH).exists())
            .andExpect(MockMvcResultMatchers.cookie().exists(TokenType.REFRESH.getName()))
            .andExpect(MockMvcResultMatchers.cookie().httpOnly(TokenType.REFRESH.getName(), true))
            .andExpect(
                MockMvcResultMatchers.cookie()
                    .maxAge(
                        TokenType.REFRESH.getName(),
                        Math.toIntExact(refreshTokenDuration.toSeconds())))
            .andReturn();

    String encryptedAccessToken = accessTokenOf(mvcResult);
    Assertions.assertFalse(jwtService.isValidJwtToken(encryptedAccessToken));

    String originalAccessToken = encryptionService.decrypt(encryptedAccessToken);
    Assertions.assertTrue(jwtService.isValidJwtToken(originalAccessToken));
  }

  @Test
  void loginWithInvalidCredentialsReturnsUnauthorized(TestInfo testInfo) throws Exception {
    var loginRequest =
        new LoginRequest(storedUser.getUsername(), testInfo.getDisplayName(), null, null);

    performRequest(MockMvcRequestBuilders.post(loginUri), TestUtils.toJson(loginRequest))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401));
  }

  /**
   * Refreshing with a valid refresh token returns a new access token.
   *
   * @throws Exception if an error occurs
   */
  @Test
  void validRefreshTokenReturnsNewAccessToken() throws Exception {
    var jwtToken = jwtService.generateJwtToken(storedUser.getUsername());
    var encryptedJwtToken = encryptionService.encrypt(jwtToken);
    var cookie = cookieService.createTokenCookie(encryptedJwtToken, TokenType.REFRESH);

    MvcResult mvcResult =
        performRequest(
                MockMvcRequestBuilders.get(refreshUri).cookie(cookieService.createCookie(cookie)),
                StringUtils.EMPTY)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    String originalAccessToken = encryptionService.decrypt(accessTokenOf(mvcResult));

    Assertions.assertTrue(jwtService.isValidJwtToken(originalAccessToken));
    Assertions.assertNotEquals(jwtToken, originalAccessToken);
  }

  @Test
  void invalidEncryptedRefreshTokenIsRejected(TestInfo testInfo) throws Exception {
    var cookie = cookieService.createTokenCookie(testInfo.getDisplayName(), TokenType.REFRESH);

    performRequest(
            MockMvcRequestBuilders.get(refreshUri).cookie(cookieService.createCookie(cookie)),
            StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().is4xxClientError());
  }

  @Test
  void refreshTokenWithBadSignatureIsRejected(TestInfo testInfo) throws Exception {
    var jwt = jwtService.generateJwtToken(testInfo.getDisplayName());
    var badSignatureJwtToken = JwtUtils.generateTestJwtToken(jwt, JwtTokenType.BAD_SIGNATURE);
    var encryptedJwtToken = encryptionService.encrypt(badSignatureJwtToken);

    var cookie = cookieService.createTokenCookie(encryptedJwtToken, TokenType.REFRESH);

    performRequest(
            MockMvcRequestBuilders.get(refreshUri).cookie(cookieService.createCookie(cookie)),
            StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().is4xxClientError());
  }

  @Test
  void logoutClearsRefreshCookie() throws Exception {
    performRequest(
            MockMvcRequestBuilders.delete(logoutUri)
                .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(storedUser.getUsername())),
            StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.cookie().exists(TokenType.REFRESH.getName()))
        .andExpect(
            MockMvcResultMatchers.cookie().value(TokenType.REFRESH.getName(), StringUtils.EMPTY));
  }

  private ResultActions performRequest(MockHttpServletRequestBuilder request, String content)
      throws Exception {
    return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(content));
  }

  private static String accessTokenOf(MvcResult mvcResult) throws Exception {
    return JsonPath.read(mvcResult.getResponse().getContentAsString(), ACCESS_TOKEN_PATH);
  }
}
