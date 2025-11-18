package com.kavun.web.advice;

import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise-grade global exception handler for REST API.
 * Handles all exceptions and returns consistent ApiResponse format.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 2.0
 */
@Slf4j
@ControllerAdvice
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
     * Overrides Spring's default handling to return ApiResponse format.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            org.springframework.http.HttpHeaders headers,
            org.springframework.http.HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        Map<String, List<String>> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new java.util.ArrayList<>()).add(errorMessage);
        });

        String path = request.getDescription(false).replace("uri=", "");
        LOG.warn("Validation failed at {}: {}", path, errors);
        ApiResponse<Object> response = ApiResponse.validationError(errors, path);
        return ResponseEntity.status(response.getStatus()).body(response);
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
     * Handles unauthorized access exceptions.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        LOG.warn("Access denied: {} at {}", ex.getMessage(), request.getRequestURI());
        ApiResponse<Object> response = ApiResponse.error(
            ResponseCode.FORBIDDEN,
            "Access denied",
            request.getRequestURI()
        );
        return response.toResponseEntity();
    }
}
