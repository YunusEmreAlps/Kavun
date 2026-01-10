package com.kavun.annotation.impl;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.persistent.domain.user.PageAction;
import com.kavun.backend.persistent.repository.PageActionRepository;
import com.kavun.backend.service.user.PermissionCheckService;
import com.kavun.enums.HttpMethod;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.util.core.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
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
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionCheckService permissionCheckService;
    private final PageActionRepository pageActionRepository;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        try {
            // Get current user ID
            UserDto userDto = SecurityUtils.getAuthorizedUserDto();
            if (userDto == null) {
                throw new AccessDeniedException("User not authenticated");
            }

            // Get HTTP request details
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes()).getRequest();

            // Determine endpoint and method
            String endpoint = getEndpoint(joinPoint, requirePermission, request);
            String httpMethod = getHttpMethod(joinPoint, requirePermission, request);

            LOG.debug("Checking permission for user {} on {} {}", userDto.getId(), httpMethod, endpoint);

            // Find matching PageAction
            Optional<PageAction> pageActionOpt = pageActionRepository
                    .findByApiEndpointAndHttpMethodAndDeletedFalse(
                            endpoint,
                            HttpMethod.valueOf(httpMethod)
                    );

            if (pageActionOpt.isEmpty()) {
                LOG.warn("No PageAction found for {} {}", httpMethod, endpoint);
                throw new AccessDeniedException("No permission configuration found for this endpoint");
            }

            // Check permission
            boolean hasPermission = permissionCheckService.hasPermissionByUserId(
                    userDto.getId(),
                    pageActionOpt.get()
            );

            if (!hasPermission) {
                LOG.warn("User {} denied access to {} {}", userDto.getId(), httpMethod, endpoint);
                throw new AccessDeniedException(requirePermission.message());
            }

            LOG.debug("User {} granted access to {} {}", userDto.getId(), httpMethod, endpoint);

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error checking permission", e);
            throw new AccessDeniedException("Error checking permission: " + e.getMessage());
        }
    }

    /**
     * Determine the endpoint from annotation, method, or request
     */
    private String getEndpoint(JoinPoint joinPoint, RequirePermission requirePermission,
                               HttpServletRequest request) {
        // 1. Check annotation
        if (!requirePermission.endpoint().isEmpty()) {
            return requirePermission.endpoint();
        }

        // 2. Check method annotations
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String path = extractPathFromMethodAnnotations(method);
        if (path != null) {
            return path;
        }

        // 3. Fallback to request URI
        return request.getRequestURI();
    }

    /**
     * Determine HTTP method from annotation, method, or request
     */
    private String getHttpMethod(JoinPoint joinPoint, RequirePermission requirePermission,
                                 HttpServletRequest request) {
        // 1. Check annotation
        if (!requirePermission.method().isEmpty()) {
            return requirePermission.method();
        }

        // 2. Check method annotations
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String httpMethod = extractHttpMethodFromAnnotations(method);
        if (httpMethod != null) {
            return httpMethod;
        }

        // 3. Fallback to request method
        return request.getMethod();
    }

    /**
     * Extract path from Spring MVC annotations
     */
    private String extractPathFromMethodAnnotations(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            String[] paths = method.getAnnotation(GetMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            String[] paths = method.getAnnotation(PostMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            String[] paths = method.getAnnotation(PutMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            String[] paths = method.getAnnotation(DeleteMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            String[] paths = method.getAnnotation(PatchMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            String[] paths = method.getAnnotation(RequestMapping.class).value();
            return paths.length > 0 ? paths[0] : null;
        }
        return null;
    }

    /**
     * Extract HTTP method from Spring MVC annotations
     */
    private String extractHttpMethodFromAnnotations(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMethod[] methods = method.getAnnotation(RequestMapping.class).method();
            return methods.length > 0 ? methods[0].name() : null;
        }
        return null;
    }
}
