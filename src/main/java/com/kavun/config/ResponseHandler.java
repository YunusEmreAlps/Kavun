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
