package com.kavun.config;

import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Enterprise-grade response handler for consistent API responses.
 * Wraps all controller responses in ApiResponse for standardization.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 2.0
 */
@Slf4j
@RestControllerAdvice
@Order(10)
public class ResponseHandler implements ResponseBodyAdvice<Object> {
    private static final String DEFAULT_PATH = "";
    private static final String NOT_FOUND_MESSAGE = "Resource not found";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return converterType.isAssignableFrom(MappingJackson2HttpMessageConverter.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        String path = getRequestPath(request);

        // Exclude Swagger/OpenAPI endpoints from wrapping
        if (isSwaggerPath(path)) {
            return body;
        }

        // Only wrap JSON responses and skip byte arrays
        if (!MediaType.APPLICATION_JSON.includes(selectedContentType) || body instanceof byte[]) {
            return body;
        }

        // If it's already an ApiResponse, skip wrapping
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        // Handle regular responses
        HttpStatus status = getHttpStatus(response);

        if (body == null) {
            return ApiResponse.error(
                    status.is2xxSuccessful() ? ResponseCode.NOT_FOUND : ResponseCode.INTERNAL_ERROR,
                    NOT_FOUND_MESSAGE,
                    path);
        }

        // Generate appropriate response code
        ResponseCode responseCode = determineResponseCode(returnType, status);

        return ApiResponse.success(responseCode, body, path);
    }

    /**
     * Determines the appropriate ResponseCode based on method name and HTTP status.
     */
    private ResponseCode determineResponseCode(MethodParameter returnType, HttpStatus status) {
        if (!status.is2xxSuccessful()) {
            return ResponseCode.INTERNAL_ERROR;
        }

        var method = returnType.getMethod();
        if (method == null) {
            return ResponseCode.SUCCESS;
        }

        String methodName = method.getName().toLowerCase();

        if (methodName.contains("create") || methodName.contains("save") || methodName.contains("add")) {
            return ResponseCode.CREATED;
        }
        if (methodName.contains("update") || methodName.contains("modify") || methodName.contains("edit")) {
            return ResponseCode.UPDATED;
        }
        if (methodName.contains("delete") || methodName.contains("remove")) {
            return ResponseCode.DELETED;
        }
        if (methodName.contains("get") || methodName.contains("find") || methodName.contains("retrieve")) {
            return ResponseCode.RETRIEVED;
        }

        return ResponseCode.SUCCESS;
    }

    /**
     * Global exception handler for non-database exceptions.
     * Database exceptions are handled separately in DatabaseExceptionHandler.
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleGeneralException(Exception ex, WebRequest request) {
        // Check if it's a database exception - if so, don't handle it here
        if (isDatabaseException(ex)) {
            LOG.debug("Database exception detected, letting DatabaseExceptionHandler handle it: {}",
                    ex.getClass().getSimpleName());
            throw new RuntimeException(ex); // Re-throw as unchecked to let DatabaseExceptionHandler handle it
        }

        LOG.error("General Exception (non-database): {}", ex.getMessage(), ex);
        String path = request != null ? request.getContextPath() : DEFAULT_PATH;
        return ApiResponse.error(ResponseCode.INTERNAL_ERROR,
                "An error occurred. Please try again.", path);
    }

    private boolean isDatabaseException(Exception ex) {
        String className = ex.getClass().getName();
        String simpleName = ex.getClass().getSimpleName();
        String message = ex.getMessage();

        boolean isDbClass = className.contains("SQLException") ||
                className.contains("DataAccessException") ||
                className.contains("PersistenceException") ||
                className.contains("TransactionSystemException") ||
                className.contains("TransactionException") ||
                className.contains("JpaSystemException") ||
                className.contains("HibernateException") ||
                className.contains("ConstraintViolationException") ||
                className.contains("RollbackException") ||
                className.contains("EntityNotFoundException") ||
                className.contains("EmptyResultDataAccessException") ||
                className.contains("DataIntegrityViolationException") ||
                simpleName.contains("SQL") ||
                simpleName.contains("Transaction") ||
                simpleName.contains("Persistence") ||
                simpleName.contains("Hibernate") ||
                simpleName.contains("JPA");

        boolean isDbMessage = message != null && (message.toLowerCase().contains("transaction") ||
                message.toLowerCase().contains("commit") ||
                message.toLowerCase().contains("rollback") ||
                message.toLowerCase().contains("constraint") ||
                message.toLowerCase().contains("sql") ||
                message.toLowerCase().contains("database") ||
                message.toLowerCase().contains("hibernate") ||
                message.toLowerCase().contains("jpa") ||
                message.toLowerCase().contains("persistence"));

        boolean isDatabase = isDbClass || isDbMessage;

        if (isDatabase) {
            LOG.debug("Identified as database exception: {} - Class: {}, Message contains DB terms: {}",
                    simpleName, isDbClass, isDbMessage);
        }

        return isDatabase;
    }

    // ... rest of the existing methods remain the same ...

    private HttpStatus getHttpStatus(ServerHttpResponse response) {
        if (!(response instanceof ServletServerHttpResponse servletResponse)) {
            return HttpStatus.OK;
        }

        int statusCode = servletResponse.getServletResponse().getStatus();

        // Handle invalid status codes
        if (statusCode <= 0) {
            return HttpStatus.OK;
        }

        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.OK;
    }

    private String getRequestPath(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return DEFAULT_PATH;
        }

        String uri = servletRequest.getServletRequest().getRequestURI();
        return uri != null ? uri : DEFAULT_PATH;
    }

    private boolean isSwaggerPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/actuator") ||
                path.contains("/api-docs");
    }
}
