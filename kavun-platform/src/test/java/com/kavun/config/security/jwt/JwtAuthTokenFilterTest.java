package com.kavun.config.security.jwt;

import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.backend.service.user.UserSessionService;
import com.kavun.enums.TokenType;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthTokenFilterTest {

  private static final String ENCRYPTED_TOKEN = "encrypted-token";
  private static final String JWT = "jwt";
  private static final Long USER_ID = 7L;
  private static final Long SESSION_ID = 42L;

  @Mock private transient JwtService jwtService;

  @Mock private transient EncryptionService encryptionService;

  @Mock private transient UserDetailsService userDetailsService;

  @Mock private transient UserSessionService userSessionService;

  @InjectMocks private transient JwtAuthTokenFilter jwtAuthTokenFilter;

  private transient MockHttpServletRequest request;
  private transient MockFilterChain filterChain;
  private transient User user;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    request = new MockHttpServletRequest();
    filterChain = new MockFilterChain();
    user = UserUtils.createUser(true);
    user.setId(USER_ID);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authenticatesTokenOfActiveSession() throws Exception {
    givenValidToken(SESSION_ID.toString());
    Mockito.when(userSessionService.isActive(SESSION_ID, USER_ID)).thenReturn(true);

    doFilter();

    Assertions.assertTrue(SecurityUtils.isAuthenticated());
    Assertions.assertNotNull(filterChain.getRequest());
  }

  @Test
  void rejectsTokenOfLoggedOutSession() throws Exception {
    givenValidToken(SESSION_ID.toString());
    Mockito.when(userSessionService.isActive(SESSION_ID, USER_ID)).thenReturn(false);

    doFilter();

    Assertions.assertFalse(SecurityUtils.isAuthenticated());
    Assertions.assertNotNull(filterChain.getRequest());
  }

  @Test
  void rejectsTokenWithoutSession() throws Exception {
    givenValidToken(null);

    doFilter();

    Assertions.assertFalse(SecurityUtils.isAuthenticated());
  }

  @Test
  void rejectsTokenOfDisabledUser() throws Exception {
    user.setEnabled(false);
    givenValidToken(SESSION_ID.toString());

    doFilter();

    Assertions.assertFalse(SecurityUtils.isAuthenticated());
    Mockito.verifyNoInteractions(userSessionService);
  }

  @Test
  void rejectsInvalidToken() throws Exception {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ENCRYPTED_TOKEN);
    Mockito.when(jwtService.getJwtToken(request, false)).thenReturn(ENCRYPTED_TOKEN);
    Mockito.when(encryptionService.decrypt(ENCRYPTED_TOKEN)).thenReturn(JWT);
    Mockito.when(jwtService.isValidJwtToken(JWT)).thenReturn(false);

    doFilter();

    Assertions.assertFalse(SecurityUtils.isAuthenticated());
    Mockito.verifyNoInteractions(userDetailsService);
  }

  @Test
  void ignoresTokenSentAsCookie() throws Exception {
    request.setCookies(new Cookie(TokenType.ACCESS.getName(), ENCRYPTED_TOKEN));

    doFilter();

    Assertions.assertFalse(SecurityUtils.isAuthenticated());
    Mockito.verify(encryptionService, Mockito.never()).decrypt(ArgumentMatchers.anyString());
  }

  private void givenValidToken(String sessionId) {
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ENCRYPTED_TOKEN);
    Mockito.when(jwtService.getJwtToken(request, false)).thenReturn(ENCRYPTED_TOKEN);
    Mockito.when(encryptionService.decrypt(ENCRYPTED_TOKEN)).thenReturn(JWT);
    Mockito.when(jwtService.isValidJwtToken(JWT)).thenReturn(true);
    Mockito.when(jwtService.getUsernameFromToken(JWT)).thenReturn(user.getUsername());
    Mockito.when(jwtService.getSessionIdFromToken(JWT)).thenReturn(sessionId);
    Mockito.when(userDetailsService.loadUserByUsername(user.getUsername()))
        .thenReturn(UserDetailsBuilder.buildUserDetails(user));
  }

  private void doFilter() throws Exception {
    jwtAuthTokenFilter.doFilter(request, new MockHttpServletResponse(), filterChain);
  }
}
