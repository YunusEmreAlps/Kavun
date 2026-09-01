package com.kavun.web.advice;

import com.kavun.constant.base.BaseConstants;
import com.kavun.exception.EncryptionException;
import com.kavun.exception.InvalidFileFormatException;
import com.kavun.exception.InvalidServiceRequestException;
import com.kavun.exception.ResourceUnavailableException;
import com.kavun.exception.StorageException;
import com.kavun.exception.UnAuthorizedActionException;
import com.kavun.exception.VirusDetectedException;
import com.kavun.exception.user.CaptchaGenerationException;
import com.kavun.exception.user.CaptchaValidationException;
import com.kavun.exception.user.EmailServiceException;
import com.kavun.exception.user.OtpGenerationException;
import com.kavun.exception.user.OtpValidationException;
import com.kavun.exception.user.SmsServiceException;
import com.kavun.exception.user.UserAlreadyExistsException;
import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
        LOG.warn("Conflict exception at {}", request.getRequestURI(), ex);
        ApiResponse<Object> response = ApiResponse.error(
            ResponseCode.CONFLICT,
            ex.getMessage(),
            request.getRequestURI()
        );
        return response.toResponseEntity();
    }

    // ==================== User Domain Exceptions ====================

    /**
     * Handles duplicate username/email errors raised anywhere in the user domain (service layer),
     * so controllers don't need to catch and re-check exception messages themselves.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    protected ResponseEntity<ApiResponse<Object>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("User already exists at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.CONFLICT,
                ex.getMessage(),
                path);
        return response.toResponseEntity();
    }

    // ==================== Domain / Business Exceptions ====================
    //
    // These exceptions carry a @ResponseStatus annotation on the exception class itself, but that
    // annotation is only consulted by ResponseStatusExceptionResolver, which never runs because
    // ExceptionHandlerExceptionResolver (this class) resolves every exception first via the
    // catch-all handleGeneralException() below. Each domain exception therefore needs an explicit
    // handler here to actually reach the status/message its own annotation declares - without one
    // it silently falls through to a generic 500.

    /**
     * Handles domain exceptions caused by bad client input (invalid OTP/CAPTCHA, unsupported or
     * infected file upload) rather than a server-side fault.
     */
    @ExceptionHandler(value = {
            OtpValidationException.class,
            CaptchaValidationException.class,
            InvalidFileFormatException.class,
            VirusDetectedException.class
    })
    protected ResponseEntity<ApiResponse<Object>> handleClientInputException(
            RuntimeException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("{} at {}: {}", ex.getClass().getSimpleName(), path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.BAD_REQUEST, ex.getMessage(), path);
        return response.toResponseEntity();
    }

    /**
     * Handles unauthorized business actions (distinct from authentication failures, which are
     * handled separately below).
     */
    @ExceptionHandler(UnAuthorizedActionException.class)
    protected ResponseEntity<ApiResponse<Object>> handleUnAuthorizedActionException(
            UnAuthorizedActionException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Unauthorized action at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.UNAUTHORIZED, ex.getMessage(), path);
        return response.toResponseEntity();
    }

    /**
     * Handles resources that the business layer could not locate.
     */
    @ExceptionHandler(ResourceUnavailableException.class)
    protected ResponseEntity<ApiResponse<Object>> handleResourceUnavailableException(
            ResourceUnavailableException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Resource unavailable at {}: {}", path, ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.NOT_FOUND, ex.getMessage(), path);
        return response.toResponseEntity();
    }

    /**
     * Handles S3 storage unavailability (thrown from circuit breaker fallbacks), mirroring the
     * CallNotPermittedException handling below.
     */
    @ExceptionHandler(StorageException.class)
    protected ResponseEntity<ApiResponse<Object>> handleStorageException(
            StorageException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.error("Storage error at {}: {}", path, ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.SERVICE_UNAVAILABLE, ex.getMessage(), path);
        return response.toResponseEntity();
    }

    /**
     * Handles internal service failures (email/SMS delivery, encryption, OTP/CAPTCHA generation) -
     * server-side faults rather than bad client input.
     */
    @ExceptionHandler(value = {
            EmailServiceException.class,
            SmsServiceException.class,
            CaptchaGenerationException.class,
            OtpGenerationException.class,
            EncryptionException.class,
            InvalidServiceRequestException.class
    })
    protected ResponseEntity<ApiResponse<Object>> handleServiceFailureException(
            RuntimeException ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.error("{} at {}: {}", ex.getClass().getSimpleName(), path, ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.INTERNAL_ERROR, ex.getMessage(), path);
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
     * Handles validation errors from @Validated on @RequestParam/@PathVariable method arguments
     * (bean validation outside a @RequestBody, e.g. @Min/@Pattern on a query param).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, List<String>> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String fieldName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.computeIfAbsent(fieldName, k -> new java.util.ArrayList<>()).add(violation.getMessage());
        }

        String path = request.getRequestURI();
        LOG.warn("Constraint violation at {}: {}", path, errors);
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
        LOG.error("Malformed JSON request at {}", path, ex);
        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.BAD_REQUEST,
                "Malformed JSON request. Please check your request body.",
                path);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles requests made with an HTTP method the endpoint doesn't support (e.g. POST-only route
     * called with GET). Without this override, the parent class's default handling returns a bare
     * ProblemDetail instead of the app's ApiResponse envelope.
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);
        LOG.warn("Method not supported at {}: {}", path, ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.METHOD_NOT_ALLOWED, ex.getMessage(), path);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Handles requests with an unsupported Content-Type.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);
        LOG.warn("Media type not supported at {}: {}", path, ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), path);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Handles a required request parameter that the client omitted.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);
        LOG.warn("Missing request parameter at {}: {}", path, ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.BAD_REQUEST, ex.getMessage(), path);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Handles a path variable or request parameter that could not be converted to the expected
     * type (e.g. a non-numeric value for a Long path variable).
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            org.springframework.beans.TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getRequestPath(request);
        LOG.warn("Type mismatch at {}: {}", path, ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getPropertyName() + "'",
                path);
        return ResponseEntity.status(response.getStatus()).body(response);
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
                BaseConstants.RESOURCE_NOT_FOUND,
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
        LOG.error("Data integrity violation at {}", path, ex);

        String message = BaseConstants.DATA_INTEGRITY_VIOLATION;
        if (ex.getMessage() != null) {
            if (ex.getMessage().toLowerCase().contains("duplicate key")) {
                message = BaseConstants.VALUE_ALREADY_EXISTS;
            } else if (ex.getMessage().toLowerCase().contains("foreign key")) {
                message = BaseConstants.VALUE_REFERENCED;
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
                BaseConstants.DATABASE_ERROR,
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
        LOG.warn("Authentication failed at {}", path, ex);

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.UNAUTHORIZED,
                BaseConstants.AUTHENTICATION_FAILED + ex.getMessage(),
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
                BaseConstants.INVALID_CREDENTIALS,
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
            BaseConstants.RESOURCE_NOT_FOUND,
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
                BaseConstants.INSUFFICIENT_PERMISSIONS,
                path);
        return response.toResponseEntity();
    }

    // ==================== Resilience Exceptions ====================

    /**
     * Handles calls rejected by an open circuit breaker (integration temporarily unavailable).
     */
    @ExceptionHandler(CallNotPermittedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleCallNotPermittedException(
            CallNotPermittedException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        LOG.warn("Circuit breaker '{}' is open, rejecting call at {}", ex.getCausingCircuitBreakerName(), path);

        ApiResponse<Object> response = ApiResponse.error(
                ResponseCode.SERVICE_UNAVAILABLE,
                BaseConstants.SERVICE_UNAVAILABLE,
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
                BaseConstants.UNEXPECTED_ERROR,
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
