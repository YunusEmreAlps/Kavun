package com.kavun.config.security.jwt;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.backend.service.user.UserSessionService;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates API requests from the bearer access token in the Authorization header.
 *
 * <p>A token is only accepted while its user can still sign in (enabled, not locked, expired or
 * deleted) and the session it was issued for is still active, so disabling a user or logging out
 * takes effect immediately instead of when the token expires. Tokens are never read from cookies:
 * CSRF protection is off for the stateless API, which is only safe for header-borne credentials.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthTokenFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final EncryptionService encryptionService;
  private final UserDetailsService userDetailsService;
  private final UserSessionService userSessionService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    var jwt = jwtService.getJwtToken(request, false);

    if (StringUtils.isNotBlank(jwt)) {
      try {
        var accessToken = encryptionService.decrypt(jwt);

        if (StringUtils.isNotBlank(accessToken) && jwtService.isValidJwtToken(accessToken)) {
          var username = jwtService.getUsernameFromToken(accessToken);
          var userDetails = userDetailsService.loadUserByUsername(username);
          var sessionId = jwtService.getSessionIdFromToken(accessToken);

          if (canSignIn(userDetails) && isActiveSession(sessionId, userDetails)) {
            SecurityUtils.authenticateUser(request, userDetails);
          } else {
            LOG.debug("Rejected token of {}: account unusable or session not active", username);
          }
        }
      } catch (Exception e) {
        // Invalid or malformed token - log and continue without authentication
        LOG.warn("Failed to decrypt or validate token: {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }

  private static boolean canSignIn(UserDetails userDetails) {
    return userDetails.isEnabled()
        && userDetails.isAccountNonLocked()
        && userDetails.isAccountNonExpired()
        && userDetails.isCredentialsNonExpired();
  }

  private boolean isActiveSession(String sessionId, UserDetails userDetails) {
    return NumberUtils.isDigits(sessionId)
        && userDetails instanceof UserDetailsBuilder user
        && userSessionService.isActive(Long.valueOf(sessionId), user.getId());
  }
}
