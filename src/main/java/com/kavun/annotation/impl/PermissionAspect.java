package com.kavun.annotation.impl;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.service.user.PermissionCheckService;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aspect for checking permissions using @RequirePermission annotation.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private final PermissionCheckService permissionCheckService;
    private final PageActionRepository pageActionRepository;

    @Value("${security.permission.admin-bypass-enabled:true}")
    private boolean adminBypassEnabled;

    public PermissionAspect(
        PermissionCheckService permissionCheckService,
        PageActionRepository pageActionRepository) {
        this.permissionCheckService = permissionCheckService;
        this.pageActionRepository = pageActionRepository;
    }

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        try {
            // Get current user
            UserDto userDto = SecurityUtils.getAuthorizedUserDto();
            if (userDto == null) {
                throw new AccessDeniedException("User not authenticated");
            }

            // Check if user is admin and admin bypass is enabled (from properties)
            if (adminBypassEnabled && isAdmin(userDto)) {
                LOG.info("Admin user {} bypassing permission check (adminBypassEnabled={})",
                        userDto.getUsername(), adminBypassEnabled);
                return;
            }

            // Get HTTP request details
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes()).getRequest();

            String httpMethod = getHttpMethod(joinPoint, requirePermission, request);
            String requestUri = request.getRequestURI();
            String pageCodeHeader = request.getHeader("Page-Code");

            LOG.debug("Checking permission for user {} on {} {} (Page-Code header: {})",
                    userDto.getId(), httpMethod, requestUri, pageCodeHeader);

            // Check permission based on pageActions or auto-detect
            String[] pageActions = requirePermission.pageActions();

            LOG.debug("RequirePermission pageActions: {}, autoDetect: {}",
                    (pageActions != null && pageActions.length > 0) ? String.join(", ", pageActions) : "[]",
                    requirePermission.autoDetect());

            // Auto-detect page:action if enabled and pageActions is empty
            if ((pageActions == null || pageActions.length == 0) && requirePermission.autoDetect()) {
                String actionOverride = requirePermission.actionOverride();
                String autoDetectedPageAction = autoDetectPageAction(
                        pageCodeHeader, requestUri, httpMethod, actionOverride);
                if (autoDetectedPageAction != null) {
                    pageActions = new String[] { autoDetectedPageAction };
                    LOG.info("Auto-detected page:action: {}", autoDetectedPageAction);
                }
            }

            if (pageActions != null && pageActions.length > 0) {
                // PAGE:ACTION BASED PERMISSION CHECK
                LOG.debug("Checking if user has any of page:action combinations: {}",
                        String.join(", ", pageActions));

                boolean hasPermission = checkPageActionPermissions(userDto.getId(), pageActions);

                if (!hasPermission) {
                    LOG.warn("User {} denied access - no matching page:action permission found", userDto.getId());
                    throw new AccessDeniedException(requirePermission.message());
                }

                LOG.debug("User {} granted access via page:action permission", userDto.getId());
            } else {
                LOG.debug("No page:action permissions specified and auto-detect disabled");
                throw new AccessDeniedException("No pageActions specified in RequirePermission annotation");
            }

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error checking permission", e);
            throw new AccessDeniedException("Error checking permission: " + e.getMessage());
        }
    }

    /**
     * Check if user has ADMIN role using Spring Security authorities
     */
    private boolean isAdmin(UserDto userDto) {
        try {
            // Check from Spring Security context first (most reliable)
            Authentication authentication = SecurityUtils.getAuthentication();
            LOG.debug("Authentication object: {}", authentication);

            if (authentication != null && authentication.getAuthorities() != null) {
                LOG.debug("Authorities: {}", authentication.getAuthorities());
                boolean hasAdminRole = authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

                LOG.info("Admin check from authorities for user {}: {} (authorities={})",
                        userDto.getUsername(), hasAdminRole, authentication.getAuthorities());
                return hasAdminRole;
            }

            // Fallback: check UserDto's userRoles field
            if (userDto.getUserRoles() != null && !userDto.getUserRoles().isEmpty()) {
                boolean hasAdminRole = userDto.getUserRoles().stream()
                        .anyMatch(ur -> ur.getRole() != null && "ROLE_ADMIN".equals(ur.getRole().getName()));

                LOG.info("Admin check from UserDto.userRoles: {}", hasAdminRole);
                return hasAdminRole;
            }

            LOG.warn("Could not determine admin status - no authorities or userRoles found for user: {}",
                    userDto.getUsername());
            return false;
        } catch (Exception e) {
            LOG.error("Error checking admin role for user: {}", userDto.getUsername(), e);
            return false;
        }
    }

    /**
     * Auto-detect page:action from header, path, and HTTP method.
     * Priority:
     * 1. Page-Code header from frontend
     * 2. URL path extraction (/api/v1/action -> ACTION)
     * 3. actionOverride (if provided) or HTTP method mapping (GET->VIEW, POST->CREATE, etc.)
     *
     * @param pageCodeHeader Page-Code header from request
     * @param requestUri     Request URI path
     * @param httpMethod     HTTP method (GET, POST, PUT, DELETE)
     * @param actionOverride Optional action override (e.g., "APPROVE", "REJECT")
     * @return page:action string or null
     */
    private String autoDetectPageAction(String pageCodeHeader, String requestUri,
                                        String httpMethod, String actionOverride) {
        try {
            // 1. Try to get page code from header (frontend tarafından gönderilen)
            String pageCode = pageCodeHeader;

            // 2. If no header, extract from URL path
            if (pageCode == null || pageCode.trim().isEmpty()) {
                pageCode = extractPageCodeFromPath(requestUri);
            }

            if (pageCode == null || pageCode.trim().isEmpty()) {
                LOG.warn("Could not determine page code from header or path: {}", requestUri);
                return null;
            }

            // 3. Determine action: use override if provided, otherwise map from HTTP method
            String action;
            if (actionOverride != null && !actionOverride.trim().isEmpty()) {
                action = actionOverride.toUpperCase();
                LOG.debug("Using action override: {}", action);
            } else {
                action = mapHttpMethodToAction(httpMethod);
                if (action == null) {
                    LOG.warn("Could not map HTTP method to action: {}", httpMethod);
                    return null;
                }
            }

            String pageAction = pageCode.toUpperCase() + ":" + action;
            LOG.debug("Auto-detected page:action = {}:{} from uri: {}, method: {} (override: {})",
                    pageCode, action, requestUri, httpMethod, actionOverride);
            return pageAction;

        } catch (Exception e) {
            LOG.error("Error auto-detecting page:action", e);
            return null;
        }
    }

    /**
     * Extract page code from URL path.
     * Example: /api/v1/action/delete/123 -> ACTION
     *          /api/v1/user/list -> USER
     */
    private String extractPageCodeFromPath(String requestUri) {
        try {
            // Remove query parameters
            String path = requestUri.split("\\?")[0];

            // Split by /
            String[] parts = path.split("/");

            // Typically: /api/v1/{resource}/... -> resource is the page
            if (parts.length >= 4 && "api".equals(parts[1])) {
                String resource = parts[3]; // parts[0] is empty, parts[1]=api, parts[2]=v1, parts[3]=resource
                return resource.toUpperCase();
            }

            LOG.debug("Could not extract page code from path: {}", requestUri);
            return null;
        } catch (Exception e) {
            LOG.error("Error extracting page code from path: {}", requestUri, e);
            return null;
        }
    }

    /**
     * Map HTTP method to standard action codes.
     */
    private String mapHttpMethodToAction(String httpMethod) {
        if (httpMethod == null) {
            return null;
        }
        return switch (httpMethod.toUpperCase()) {
            case "GET" -> "VIEW";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "EDIT";
            case "DELETE" -> "DELETE";
            default -> null;
        };
    }

    /**
     * Check if user has permission for any of the specified page:action
     * combinations.
     * Format: "PAGE_CODE:ACTION_CODE"
     *
     * @param userId      User ID to check permissions for
     * @param pageActions Array of "PAGE_CODE:ACTION_CODE" strings
     * @return true if user has any one of the specified permissions (OR logic)
     */
    private boolean checkPageActionPermissions(Long userId, String[] pageActions) {
        for (String pageAction : pageActions) {
            String[] parts = pageAction.split(":", 2);
            if (parts.length != 2) {
                LOG.warn("Invalid pageAction format: {}. Expected 'PAGE_CODE:ACTION_CODE'", pageAction);
                continue;
            }

            String pageCode = parts[0].trim();
            String actionCode = parts[1].trim();

            LOG.debug("Checking permission for pageCode={}, actionCode={}", pageCode, actionCode);

            try {
                Optional<PageAction> pageActionOpt = pageActionRepository
                        .findActionByPageCodeAndActionCode(pageCode, actionCode);

                if (pageActionOpt.isPresent()) {
                    boolean hasPermission = permissionCheckService.hasPermissionByUserId(userId, pageActionOpt.get());
                    if (hasPermission) {
                        LOG.debug("User has permission for {}:{}", pageCode, actionCode);
                        return true; // User has at least one matching permission
                    }
                }
            } catch (Exception e) {
                LOG.error("Error checking permission for {}:{}", pageCode, actionCode, e);
            }
        }

        return false; // User doesn't have any of the specified permissions
    }

    /**
     * Determine HTTP method from annotation, method, or request
     */
    private String getHttpMethod(JoinPoint joinPoint, RequirePermission requirePermission,
            HttpServletRequest request) {
        // Check method annotations
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String httpMethod = extractHttpMethodFromAnnotations(method);
        if (httpMethod != null) {
            return httpMethod;
        }
        return request.getMethod();
    }

    private String extractHttpMethodFromAnnotations(Method method) {
        if (method.isAnnotationPresent(GetMapping.class))
            return "GET";
        if (method.isAnnotationPresent(PostMapping.class))
            return "POST";
        if (method.isAnnotationPresent(PutMapping.class))
            return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class))
            return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class))
            return "PATCH";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMethod[] methods = method.getAnnotation(RequestMapping.class).method();
            return methods.length > 0 ? methods[0].name() : null;
        }
        return null;
    }
}
