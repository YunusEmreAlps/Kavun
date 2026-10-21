package com.kavun.config.security.authorization;

import com.kavun.backend.service.security.permission.AccessControlService;
import com.kavun.constant.SecurityConstants;
import com.kavun.constant.base.BaseConstants;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Authorizes every API request from the access annotations of the controller method that handles
 * it. Runs after the handler is known but before the request body is read, so a caller without
 * access never reaches validation or business code. Denials surface as {@code
 * AuthenticationException} (401) or {@link AccessDeniedException} (403) through the global
 * exception handler.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class EndpointAuthorizationInterceptor implements HandlerInterceptor, WebMvcConfigurer {

  private final EndpointPolicyResolver policyResolver;
  private final AccessControlService accessControlService;

  // Registers itself, so the check cannot be wired without the code that performs it.
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns(SecurityConstants.API_ROOT_URL_MAPPING);
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true; // no controller method, e.g. an unknown route answered with 404
    }

    EndpointPolicy policy = policyResolver.resolve(handlerMethod, request.getMethod());
    if (policy.access() == EndpointPolicy.Access.PUBLIC) {
      return true;
    }

    var authentication = SecurityUtils.getAuthentication();
    if (!SecurityUtils.isAuthenticated(authentication)) {
      throw new AuthenticationCredentialsNotFoundException(BaseConstants.AUTHENTICATION_REQUIRED);
    }
    if (policy.access() == EndpointPolicy.Access.PERMISSION
        && !accessControlService.hasPermission(authentication, policy.permission())) {
      throw new AccessDeniedException("Missing permission " + policy.permission());
    }
    return true;
  }
}
