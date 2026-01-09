package com.kavun.config;

import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Enterprise-grade response handler for consistent API responses.
 * Wraps all controller responses in ApiResponse for standardization.
 *
 * Note: Exception handling is managed in RestResponseEntityExceptionHandler.
 * This class only handles successful response wrapping.
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

    /** Swagger/OpenAPI path prefixes to exclude from response wrapping. */
    private static final String[] EXCLUDED_PATH_PREFIXES = {
            "/v3/api-docs", "/swagger", "/swagger-ui", "/actuator"
    };

    /** Swagger/OpenAPI path patterns to exclude from response wrapping. */
    private static final String[] EXCLUDED_PATH_PATTERNS = { "/api-docs" };

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip StringHttpMessageConverter to avoid casting issues with wrapped ApiResponse
        // We want JSON converters to handle all responses after wrapping
        // return !converterType.getName().contains("StringHttpMessageConverter");
        return true;
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

        // Only wrap JSON-like responses (application/json, application/hal+json, etc.)
        if (!isJsonMediaType(selectedContentType)) {
            return body;
        }

        // Skip types that should not be wrapped
        if (shouldSkipWrapping(body)) {
            return body;
        }

        // Safely wrap response in try-catch to prevent errors for unexpected types
        try {
            HttpStatus status = getHttpStatus(response);
            ResponseCode responseCode = determineResponseCode(returnType, status);

            /*// If body is a String, use it as the message instead of data
            if (body instanceof String stringBody) {
                return ApiResponse.builder()
                        .status(status.value())
                        .code(responseCode.name())
                        .message(stringBody)
                        .data(new LinkedHashMap<>())
                        .path(path)
                        .build();
            }*/

            Object data = (body != null) ? body : new LinkedHashMap<>();
            return ApiResponse.success(responseCode, data, path);
        } catch (Exception e) {
            LOG.warn("Failed to wrap response, returning original body: {}", e.getMessage());
            return body;
        }
    }

    /**
     * Determines the appropriate ResponseCode based on method name and HTTP status.
     */
    private ResponseCode determineResponseCode(MethodParameter returnType, HttpStatus status) {
        // Handle non-2xx status codes based on HTTP status
        if (!status.is2xxSuccessful()) {
            return mapHttpStatusToResponseCode(status);
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
     * Maps HTTP status to appropriate ResponseCode for non-2xx responses.
     */
    private ResponseCode mapHttpStatusToResponseCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ResponseCode.NOT_FOUND;
            case BAD_REQUEST -> ResponseCode.BAD_REQUEST;
            case UNAUTHORIZED -> ResponseCode.UNAUTHORIZED;
            case FORBIDDEN -> ResponseCode.FORBIDDEN;
            case CONFLICT -> ResponseCode.CONFLICT;
            case NO_CONTENT -> ResponseCode.NO_CONTENT;
            case SERVICE_UNAVAILABLE -> ResponseCode.SERVICE_UNAVAILABLE;
            default -> ResponseCode.INTERNAL_ERROR;
        };
    }

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

    /**
     * Checks if the media type is JSON-compatible.
     */
    private boolean isJsonMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return mediaType.includes(MediaType.APPLICATION_JSON)
                || "json".equalsIgnoreCase(mediaType.getSubtype())
                || mediaType.getSubtype().endsWith("+json");
    }

    /**
     * Determines if the response body should skip wrapping.
     */
    private boolean shouldSkipWrapping(Object body) {
        return body instanceof byte[]
                || body instanceof ApiResponse<?>;
    }

    private boolean isSwaggerPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        for (String pattern : EXCLUDED_PATH_PATTERNS) {
            if (path.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}
