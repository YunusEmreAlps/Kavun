package com.kavun.config.security.jwt;

import com.kavun.backend.service.security.EncryptionService;
import com.kavun.backend.service.security.JwtService;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This is a filter base class used to guarantee a single execution per request dispatch.
 * This filter handles JWT token authentication for non-API endpoints when using local JWT.
 * When Keycloak is enabled, API endpoints (/api/**) are handled by Keycloak's BearerTokenAuthenticationFilter.
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

  @Value("${keycloak.enabled:true}")
  private boolean keycloakEnabled;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Skip this filter for API endpoints when Keycloak is enabled
    // Keycloak's BearerTokenAuthenticationFilter handles API JWT authentication
    String path = request.getRequestURI();
    if (keycloakEnabled && path.startsWith("/api/")) {
      LOG.debug("Skipping JwtAuthTokenFilter for API endpoint: {} (Keycloak handles JWT)", path);
      return true;
    }
    return false;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Get the token from the request header
    var jwt = jwtService.getJwtToken(request, false);

    if (StringUtils.isBlank(jwt)) {
      // if no Authorization token was found from the header, check the cookies.
      jwt = jwtService.getJwtToken(request, true);
    }

    if (StringUtils.isNotBlank(jwt)) {
      try {
        var accessToken = encryptionService.decrypt(jwt);

        if (StringUtils.isNotBlank(accessToken) && jwtService.isValidJwtToken(accessToken)) {
          var username = jwtService.getUsernameFromToken(accessToken);
          var userDetails = userDetailsService.loadUserByUsername(username);
          SecurityUtils.authenticateUser(request, userDetails);
        }
      } catch (Exception e) {
        // Log and continue - token might be a Keycloak JWT that shouldn't be decrypted
        LOG.debug("Failed to process JWT token: {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
