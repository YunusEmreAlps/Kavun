package com.kavun.config.security.authorization;

import com.kavun.annotation.AuthenticatedEndpoint;
import com.kavun.annotation.PublicEndpoint;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.security.permission.PageActionCode;
import com.kavun.constant.PermissionConstants.Actions;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * Resolves the {@link EndpointPolicy} of a handler method from {@link PublicEndpoint}, {@link
 * AuthenticatedEndpoint} and {@link RequirePermission} on the method and its controller. A method
 * annotation replaces the controller's, except that a method-level {@code @RequirePermission}
 * without a page uses the controller's page.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class EndpointPolicyResolver {

  private static final List<Class<? extends Annotation>> POLICY_ANNOTATIONS =
      List.of(PublicEndpoint.class, AuthenticatedEndpoint.class, RequirePermission.class);

  private static final Map<String, String> DEFAULT_ACTIONS =
      Map.of(
          "GET", Actions.VIEW,
          "HEAD", Actions.VIEW,
          "POST", Actions.CREATE,
          "PUT", Actions.EDIT,
          "PATCH", Actions.EDIT,
          "DELETE", Actions.DELETE);

  /**
   * Resolves the policy of a handler method for the given HTTP method.
   *
   * @param handlerMethod the handler method
   * @param httpMethod the HTTP method of the request
   * @return the policy
   * @throws EndpointPolicyException if the annotations are missing or contradictory
   */
  public EndpointPolicy resolve(HandlerMethod handlerMethod, String httpMethod) {
    String endpoint =
        handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    Annotation onMethod = findPolicyAnnotation(handlerMethod.getMethod(), endpoint);
    Annotation onType = findPolicyAnnotation(handlerMethod.getBeanType(), endpoint);

    Annotation effective = onMethod != null ? onMethod : onType;
    if (effective == null) {
      throw new EndpointPolicyException(
          endpoint
              + " declares no access policy; add @PublicEndpoint, @AuthenticatedEndpoint or"
              + " @RequirePermission to the method or its controller");
    }
    if (effective instanceof PublicEndpoint) {
      return EndpointPolicy.PUBLIC;
    }
    if (effective instanceof AuthenticatedEndpoint) {
      return EndpointPolicy.AUTHENTICATED;
    }

    var typePermission = onType instanceof RequirePermission rp ? rp : null;
    if (typePermission != null && StringUtils.isNotBlank(typePermission.action())) {
      throw new EndpointPolicyException(
          endpoint + ": @RequirePermission on a controller takes a page only; set actions on methods");
    }
    var methodPermission = onMethod instanceof RequirePermission rp ? rp : null;

    String page =
        methodPermission != null && StringUtils.isNotBlank(methodPermission.page())
            ? methodPermission.page()
            : typePermission == null ? null : StringUtils.trimToNull(typePermission.page());
    if (page == null) {
      throw new EndpointPolicyException(
          endpoint + " uses @RequirePermission without a page on the method or its controller");
    }

    String action =
        methodPermission != null && StringUtils.isNotBlank(methodPermission.action())
            ? methodPermission.action()
            : DEFAULT_ACTIONS.get(httpMethod.toUpperCase(Locale.ROOT));
    if (action == null) {
      throw new EndpointPolicyException(
          endpoint + " needs an explicit @RequirePermission action for HTTP " + httpMethod);
    }
    return new EndpointPolicy(EndpointPolicy.Access.PERMISSION, new PageActionCode(page, action));
  }

  private static Annotation findPolicyAnnotation(AnnotatedElement element, String endpoint) {
    List<Annotation> found =
        POLICY_ANNOTATIONS.stream()
            .<Annotation>map(type -> AnnotatedElementUtils.findMergedAnnotation(element, type))
            .filter(Objects::nonNull)
            .toList();
    if (found.size() > 1) {
      throw new EndpointPolicyException(
          endpoint
              + " declares more than one access policy: "
              + found.stream().map(a -> "@" + a.annotationType().getSimpleName()).toList());
    }
    return found.isEmpty() ? null : found.getFirst();
  }
}
