package com.kavun.web.rest.v1;

import com.kavun.IntegrationTestUtils;
import com.kavun.TestUtils;
import com.kavun.backend.persistent.repository.OtpRepository;
import com.kavun.backend.persistent.repository.UserSessionRepository;
import com.kavun.backend.service.user.OtpService;
import com.kavun.constant.AuthConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.enums.TokenType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.JwtUtils;
import com.kavun.shared.util.core.JwtUtils.JwtTokenType;
import com.kavun.web.payload.request.LoginRequest;
import com.kavun.web.payload.request.OtpVerificationRequest;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
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
  private transient String verifyOtpUri;
  private transient String loginRequestJson;
  private transient UserDto storedUser;

  @Autowired private transient UserSessionRepository userSessionRepository;
  @Autowired private transient OtpService otpService;
  @Autowired private transient OtpRepository otpRepository;

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
    verifyOtpUri = SecurityConstants.API_V1_AUTH_ROOT_URL + SecurityConstants.VERIFY_OTP;
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

  // The answer must not tell an attacker which usernames exist.
  @Test
  void loginWithUnknownUsernameLooksLikeAWrongPassword(TestInfo testInfo) throws Exception {
    var loginRequest = new LoginRequest("unknown-user", testInfo.getDisplayName(), null, null);

    performRequest(MockMvcRequestBuilders.post(loginUri), TestUtils.toJson(loginRequest))
        .andExpect(MockMvcResultMatchers.status().isUnauthorized())
        .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(AuthConstants.INVALID_CREDENTIALS));
  }

  @Test
  void signInCodeCompletesTheLoginOnlyOnce() throws Exception {
    var challenge = otpService.sendSignInCode(storedUser);
    var verification = TestUtils.toJson(new OtpVerificationRequest(challenge.id(), codeOf(challenge.id())));

    performRequest(MockMvcRequestBuilders.post(verifyOtpUri), verification)
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath(ACCESS_TOKEN_PATH).exists());

    performRequest(MockMvcRequestBuilders.post(verifyOtpUri), verification)
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  // Failed attempts must be stored even though each one ends in an error response.
  @Test
  void wrongSignInCodesUseUpTheChallenge() throws Exception {
    var challenge = otpService.sendSignInCode(storedUser);
    var code = codeOf(challenge.id());
    var wrongCode = code.equals("000000") ? "111111" : "000000";

    for (int attempt = 0; attempt < SecurityConstants.OTP_MAX_ATTEMPTS; attempt++) {
      performRequest(
              MockMvcRequestBuilders.post(verifyOtpUri),
              TestUtils.toJson(new OtpVerificationRequest(challenge.id(), wrongCode)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    performRequest(
            MockMvcRequestBuilders.post(verifyOtpUri),
            TestUtils.toJson(new OtpVerificationRequest(challenge.id(), code)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(AuthConstants.OTP_MAX_ATTEMPTS));
  }

  private String codeOf(String challengeId) {
    return otpRepository.findByPublicId(challengeId).orElseThrow().getCode();
  }

  /**
   * Refreshing with a valid refresh token returns a new access token.
   *
   * @throws Exception if an error occurs
   */
  @Test
  void validRefreshTokenReturnsNewAccessToken() throws Exception {
    var session = userSessionService.createSession(storedUser.getId(), new MockHttpServletRequest());
    var jwtToken = refreshTokenFor(session.getId());

    MvcResult mvcResult =
        performRequest(refreshRequest(jwtToken), StringUtils.EMPTY)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    String originalAccessToken = encryptionService.decrypt(accessTokenOf(mvcResult));

    Assertions.assertTrue(jwtService.isValidJwtToken(originalAccessToken));
    Assertions.assertNotEquals(jwtToken, originalAccessToken);
  }

  @Test
  void refreshTokenOfTimedOutSessionResumesIt() throws Exception {
    var session = userSessionService.createSession(storedUser.getId(), new MockHttpServletRequest());
    session.expire();
    userSessionRepository.save(session);

    performRequest(refreshRequest(refreshTokenFor(session.getId())), StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().isOk());
    Assertions.assertTrue(userSessionService.isActive(session.getId(), storedUser.getId()));
  }

  @Test
  void refreshTokenOfLoggedOutSessionIsRejected() throws Exception {
    var session = userSessionService.createSession(storedUser.getId(), new MockHttpServletRequest());
    userSessionService.logout(session.getId());

    performRequest(refreshRequest(refreshTokenFor(session.getId())), StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
  void refreshTokenWithoutSessionIsRejected() throws Exception {
    var jwtToken = jwtService.generateJwtToken(storedUser.getUsername());

    performRequest(refreshRequest(jwtToken), StringUtils.EMPTY)
        .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  private String refreshTokenFor(Long sessionId) {
    var expiration = DateUtils.addDays(new Date(), SecurityConstants.DEFAULT_TOKEN_DURATION);
    return jwtService.generateJwtToken(storedUser.getUsername(), expiration, sessionId.toString());
  }

  private MockHttpServletRequestBuilder refreshRequest(String jwtToken) {
    var cookie =
        cookieService.createTokenCookie(encryptionService.encrypt(jwtToken), TokenType.REFRESH);
    return MockMvcRequestBuilders.get(refreshUri).cookie(cookieService.createCookie(cookie));
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
                .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(storedUser)),
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
