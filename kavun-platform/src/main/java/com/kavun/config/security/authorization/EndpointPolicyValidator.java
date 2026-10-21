package com.kavun.config.security.authorization;

import com.kavun.backend.service.security.permission.PermissionCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Refuses to start the application when an API endpoint has no access policy, contradictory
 * access annotations, or requires a permission the {@link PermissionCatalog} does not declare. This
 * turns a forgotten annotation into a failed build instead of an endpoint that is silently open
 * or silently unusable.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class EndpointPolicyValidator implements SmartInitializingSingleton {

  private static final String API_PATH_PREFIX = "/api/";
  private static final List<String> ALL_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");

  private final RequestMappingHandlerMapping handlerMapping;
  private final EndpointPolicyResolver policyResolver;
  private final PermissionCatalog permissionCatalog;

  public EndpointPolicyValidator(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
      EndpointPolicyResolver policyResolver,
      PermissionCatalog permissionCatalog) {
    this.handlerMapping = handlerMapping;
    this.policyResolver = policyResolver;
    this.permissionCatalog = permissionCatalog;
  }

  @Override
  public void afterSingletonsInstantiated() {
    List<String> violations = validate();
    if (!violations.isEmpty()) {
      throw new EndpointPolicyException(
          "API endpoints with invalid access policies:\n  - " + String.join("\n  - ", violations));
    }
    LOG.info("Access policies of all API endpoints verified");
  }

  /**
   * Checks every API endpoint.
   *
   * @return a description of each problem found, empty if all endpoints are valid
   */
  public List<String> validate() {
    Set<String> violations = new TreeSet<>();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry :
        handlerMapping.getHandlerMethods().entrySet()) {
      if (!isApiMapping(entry.getKey())) {
        continue;
      }
      for (String httpMethod : httpMethods(entry.getKey())) {
        try {
          EndpointPolicy policy = policyResolver.resolve(entry.getValue(), httpMethod);
          if (policy.permission() != null && !permissionCatalog.contains(policy.permission())) {
            violations.add(
                describe(entry.getValue())
                    + " requires "
                    + policy.permission()
                    + ", which is not declared in the permission catalog");
          }
        } catch (EndpointPolicyException e) {
          violations.add(e.getMessage());
        }
      }
    }
    return new ArrayList<>(violations);
  }

  private static boolean isApiMapping(RequestMappingInfo info) {
    return info.getPatternValues().stream().anyMatch(path -> path.startsWith(API_PATH_PREFIX));
  }

  private static List<String> httpMethods(RequestMappingInfo info) {
    Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
    return methods.isEmpty() ? ALL_METHODS : methods.stream().map(RequestMethod::name).toList();
  }

  private static String describe(HandlerMethod handlerMethod) {
    return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
  }
}
