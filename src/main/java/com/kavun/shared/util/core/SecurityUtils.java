package com.kavun.shared.util.core;

import com.kavun.backend.service.impl.UserDetailsBuilder;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
   *
   * @return the user details
   */
  public static UserDetailsBuilder getAuthenticatedUserDetails() {
    if (isAuthenticated()) {
      return (UserDetailsBuilder) getAuthentication().getPrincipal();
    }
    return null;
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
