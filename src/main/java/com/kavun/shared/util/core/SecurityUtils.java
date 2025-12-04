package com.kavun.shared.util.core;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

/**
 * This utility class holds custom operations on security used in the application.
 *
 * @author Stephen
 * @version 1.0
 * @since 1.0
 */
@Slf4j
public final class SecurityUtils {

  private SecurityUtils() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }

  /**
   * Returns true if the user is authenticated.
   *
   * @param authentication the authentication object
   * @return if a user is authenticated
   */
  public static boolean isAuthenticated(Authentication authentication) {
    return Objects.nonNull(authentication)
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  /**
   * Returns true if the user is authenticated.
   *
   * @return if a user is authenticated
   */
  public static boolean isAuthenticated() {
    return isAuthenticated(getAuthentication());
  }

  /**
   * Retrieve the authentication object from the current session.
   *
   * @return authentication
   */
  public static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  /**
   * Sets the provided authentication object to the SecurityContextHolder.
   *
   * @param authentication the authentication
   */
  public static void setAuthentication(Authentication authentication) {
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  /** Clears the securityContextHolder. */
  public static void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  /**
   * Creates an authentication object with the userDetails then set authentication to
   * SecurityContextHolder.
   *
   * @param userDetails the userDetails
   */
  public static void authenticateUser(UserDetails userDetails) {
    if (Objects.nonNull(userDetails)) {
      var authorities = userDetails.getAuthorities();
      var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

      setAuthentication(authentication);
    }
  }

  /**
   * Creates an authentication object with the userDetails then set authentication to
   * SecurityContextHolder.
   *
   * @param userDetails the userDetails
   */
  public static void authenticateUser(HttpServletRequest request, UserDetails userDetails) {
    if (Objects.nonNull(request) && Objects.nonNull(userDetails)) {
      var authorities = userDetails.getAuthorities();
      var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      setAuthentication(authentication);
    }
  }

  /**
   * Creates an authentication object with the credentials then set authentication to
   * SecurityContextHolder.
   *
   * @param authenticationProvider the authentication manager
   * @param username the username
   * @param password the password
   */
  public static void authenticateUser(
      AuthenticationProvider authenticationProvider, String username, String password) {

    var authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
    var authentication = authenticationProvider.authenticate(authenticationToken);

    setAuthentication(authentication);
  }

  /**
   * Returns the user details from the authenticated object if authenticated.
   * Supports both UserDetailsBuilder (form login) and Jwt (Keycloak OAuth2).
   *
   * @return the user details or null if not authenticated or using JWT
   */
  public static UserDetailsBuilder getAuthenticatedUserDetails() {
    if (isAuthenticated()) {
      Object principal = getAuthentication().getPrincipal();
      if (principal instanceof UserDetailsBuilder) {
        return (UserDetailsBuilder) principal;
      }
      // For JWT authentication, return null - use getJwtPrincipal() instead
      return null;
    }
    return null;
  }

  /**
   * Returns the JWT from the authenticated object if using Keycloak/OAuth2.
   *
   * @return the JWT or null if not using JWT authentication
   */
  public static Jwt getJwtPrincipal() {
    if (isAuthenticated()) {
      Object principal = getAuthentication().getPrincipal();
      if (principal instanceof Jwt) {
        return (Jwt) principal;
      }
    }
    return null;
  }

  /**
   * Checks if the current authentication is JWT-based (Keycloak).
   *
   * @return true if using JWT authentication
   */
  public static boolean isJwtAuthentication() {
    return getAuthentication() instanceof JwtAuthenticationToken;
  }

  /**
   * Gets the username from either UserDetailsBuilder or JWT.
   *
   * @return the username or null if not authenticated
   */
  public static String getAuthenticatedUsername() {
    if (!isAuthenticated()) {
      return null;
    }

    Object principal = getAuthentication().getPrincipal();
    if (principal instanceof UserDetailsBuilder userDetails) {
      return userDetails.getUsername();
    } else if (principal instanceof Jwt jwt) {
      return jwt.getClaimAsString("preferred_username");
    } else if (principal instanceof String) {
      return (String) principal;
    }
    return null;
  }

  /**
   * Gets the user's public ID from either UserDetailsBuilder or JWT (sub claim).
   *
   * @return the public ID or null if not authenticated
   */
  public static String getAuthenticatedPublicId() {
    if (!isAuthenticated()) {
      return null;
    }

    Object principal = getAuthentication().getPrincipal();
    if (principal instanceof UserDetailsBuilder userDetails) {
      return userDetails.getPublicId();
    } else if (principal instanceof Jwt jwt) {
      return jwt.getSubject(); // Keycloak user ID
    }
    return null;
  }

  /**
   * Gets the user's email from either UserDetailsBuilder or JWT.
   *
   * @return the email or null if not authenticated
   */
  public static String getAuthenticatedEmail() {
    if (!isAuthenticated()) {
      return null;
    }

    Object principal = getAuthentication().getPrincipal();
    if (principal instanceof UserDetailsBuilder userDetails) {
      return userDetails.getEmail();
    } else if (principal instanceof Jwt jwt) {
      return jwt.getClaimAsString("email");
    }
    return null;
  }

  /**
   * Gets the authorities/roles from either UserDetailsBuilder or JWT.
   *
   * @return the collection of granted authorities
   */
  public static Collection<? extends GrantedAuthority> getAuthenticatedAuthorities() {
    if (!isAuthenticated()) {
      return List.of();
    }

    Authentication auth = getAuthentication();
    if (auth.getAuthorities() != null) {
      return auth.getAuthorities();
    }
    return List.of();
  }

  /**
   * Checks if the authenticated user has a specific role.
   *
   * @param role the role to check (with or without ROLE_ prefix)
   * @return true if the user has the role
   */
  public static boolean hasRole(String role) {
    if (!isAuthenticated()) {
      return false;
    }

    String roleToCheck = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    return getAuthenticatedAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals(roleToCheck));
  }

  /**
   * Retrieve the authenticated user from the current session.
   *
   * @return the userDetailsBuilder
   */
  public static UserDto getAuthorizedUserDto() {
    return UserUtils.convertToUserDto(getAuthorizedUserDetails());
  }

  /**
   * Retrieve the authenticated user from the current session.
   *
   * @return the userDetailsBuilder
   */
  public static UserDetailsBuilder getAuthorizedUserDetails() {
    var userDetails = getAuthenticatedUserDetails();
    if (Objects.isNull(userDetails)) {
      LOG.warn(ErrorConstants.UNAUTHORIZED_ACCESS);
      return null;
    }
    return userDetails;
  }

  /**
   * Logout the user from the system and clear all cookies from request and response.
   *
   * @param request the request
   * @param response the response
   */
  public static void logout(HttpServletRequest request, HttpServletResponse response) {

    String rememberMeCookieKey = AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY;
    CookieClearingLogoutHandler logoutHandler =
        new CookieClearingLogoutHandler(rememberMeCookieKey);

    SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
    logoutHandler.logout(request, response, null);
    securityContextLogoutHandler.logout(request, response, null);
  }

  /**
   * Validates that the user is neither disabled, locked nor expired.
   *
   * @param userDetails the user details
   */
  public static void validateUserDetailsStatus(UserDetails userDetails) {
    LOG.debug(UserConstants.USER_DETAILS_DEBUG_MESSAGE, userDetails);

    if (!userDetails.isEnabled()) {
      throw new DisabledException(UserConstants.USER_DISABLED_MESSAGE);
    }
    if (!userDetails.isAccountNonLocked()) {
      throw new LockedException(UserConstants.USER_LOCKED_MESSAGE);
    }
    if (!userDetails.isAccountNonExpired()) {
      throw new AccountExpiredException(UserConstants.USER_EXPIRED_MESSAGE);
    }
    if (!userDetails.isCredentialsNonExpired()) {
      throw new CredentialsExpiredException(UserConstants.USER_CREDENTIALS_EXPIRED_MESSAGE);
    }
  }
}
