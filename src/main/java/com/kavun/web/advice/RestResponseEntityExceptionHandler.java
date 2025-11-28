package com.kavun.web.advice;

import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized exception handler for all application exceptions.
 * Provides consistent error responses across the application.
 *
 * Handles:
 * - Validation errors
 * - Database exceptions (integrity, access)
 * - Security exceptions (authentication, authorization)
 * - Static resource errors (Swagger, CSS, JS)
 * - General application exceptions
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 2.0
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles IllegalArgumentException and IllegalStateException thrown by the REST API.
     */
    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    protected ResponseEntity<ApiResponse<Object>> handleConflict(RuntimeException ex, HttpServletRequest request) {
        LOG.warn("Conflict exception: {} at {}", ex.getMessage(), request.getRequestURI());
        ApiResponse<Object> response = ApiResponse.error(
            ResponseCode.CONFLICT,
            ex.getMessage(),
            request.getRequestURI()
        );
        return response.toResponseEntity();
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Returns field-level error details in ApiResponse format.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, List<String>> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new java.util.ArrayList<>()).add(errorMessage);
        });

        String path = getRequestPath(request);
        LOG.warn("Validation failed at {}: {}", path, errors);
        ApiResponse<Object> response = ApiResponse.validationError(errors, path);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Handles malformed JSON requests.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);
        LOG.error("Malformed JSON request at {}: {}", path, ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.BAD_REQUEST,
                "Malformed JSON request. Please check your request body.",
                path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Static Resource Exceptions ====================

    /**
     * Handles static resource not found (Swagger, CSS, JS, images, etc.)
     * Overrides parent class method to provide custom handling.
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);

        // Don't wrap static resources - let Spring handle them with default behavior
        if (isStaticResourcePath(path)) {
            LOG.debug("Static resource not found (normal): {}", path);
            return super.handleNoResourceFoundException(ex, headers, status, request);
        }

        // For API endpoints, return ApiResponse
        LOG.warn("API resource not found: {}", path);
        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.NOT_FOUND,
                "The requested resource was not found",
                path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ==================== Database Exceptions ====================

    /**
     * Handles data integrity violations (duplicate keys, foreign key constraints).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.error("Data integrity violation at {}: {}", path, ex.getMessage());

        String message = "Data integrity violation";
        if (ex.getMessage() != null) {
            if (ex.getMessage().toLowerCase().contains("duplicate key")) {
                message = "A record with this value already exists";
            } else if (ex.getMessage().toLowerCase().contains("foreign key")) {
                message = "Cannot delete record - it is referenced by other records";
            }
        }

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.CONFLICT,
                message,
                path);
        return response.toResponseEntity();
    }

    /**
     * Handles general database access exceptions.
     */
    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.error("Database access error at {}: {}", path, ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.INTERNAL_ERROR,
                "Database error occurred. Please try again later.",
                path);
        return response.toResponseEntity();
    }

    // ==================== Security Exceptions ====================

    /**
     * Handles authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Authentication failed at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.UNAUTHORIZED,
                "Authentication failed: " + ex.getMessage(),
                path);
        return response.toResponseEntity();
    }

    /**
     * Handles bad credentials (invalid username/password).
     */
    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Bad credentials at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.UNAUTHORIZED,
                "Invalid username or password",
                path);
        return response.toResponseEntity();
    }

    /**
     * Handles resource not found exceptions.
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    protected ResponseEntity<ApiResponse<Object>> handleNotFound(Exception ex, HttpServletRequest request) {
        LOG.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());
        ApiResponse<Object> response = ApiResponse.error(
            ResponseCode.NOT_FOUND,
            ex.getMessage(),
            request.getRequestURI()
        );
        return response.toResponseEntity();
    }

    /**
     * Handles access denied (insufficient permissions).
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Access denied at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.FORBIDDEN,
                "You don't have permission to access this resource",
                path);
        return response.toResponseEntity();
    }

    // ==================== General Exceptions ====================

    /**
     * Handles all uncaught exceptions as a fallback.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.error("Unhandled exception at {}: {}", path, ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later.",
                path);
        return response.toResponseEntity();
    }

    // ==================== Utility Methods ====================

    /**
     * Extracts the request path from WebRequest.
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getRequest();
            return httpRequest.getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Checks if the path is a static resource (Swagger, CSS, JS, images, etc.).
     */
    private boolean isStaticResourcePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/actuator") ||
                path.contains("/api-docs") ||
                path.contains("/static/") ||
                path.contains("/webjars/") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".html") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".ico") ||
                path.endsWith(".svg") ||
                path.endsWith(".woff") ||
                path.endsWith(".woff2") ||
                path.endsWith(".ttf") ||
                path.endsWith(".map");
    }
}
