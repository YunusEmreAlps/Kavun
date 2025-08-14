package com.kavun.config;

import com.kavun.constant.base.BaseConstants;
import com.kavun.web.payload.response.CustomResponse;

import org.springframework.core.MethodParameter;
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
 * Controller advice to wrap all controller responses in CustomResponse for
 * consistency.
 */
@RestControllerAdvice
public class ResponseHandler implements ResponseBodyAdvice<Object> {
    private static final String DEFAULT_PATH = "";
    private static final String NOT_FOUND_MESSAGE = "Resource not found";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Only apply to JSON responses to avoid interfering with byte[] or other
        // non-JSON content
        return converterType.isAssignableFrom(MappingJackson2HttpMessageConverter.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        String path = getRequestPath(request);

        // Exclude Swagger/OpenAPI endpoints from wrapping
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger") || path.startsWith("/swagger-ui")) {
            return body;
        }

        if (!MediaType.APPLICATION_JSON.includes(selectedContentType) || body instanceof byte[]) {
            return body;
        }

        if (body instanceof CustomResponse) {
            return body;
        }

        HttpStatus status = getHttpStatus(response);

        if (body == null) {
            return CustomResponse.error(status != HttpStatus.OK ? status : HttpStatus.NOT_FOUND,
                    NOT_FOUND_MESSAGE, path);
        }

        String message = generateMessage(returnType, body);
        return CustomResponse.of(status, body, message, path);
    }

    private HttpStatus getHttpStatus(ServerHttpResponse response) {
        if (!(response instanceof ServletServerHttpResponse servletResponse)) {
            return HttpStatus.OK;
        }
        int statusCode = servletResponse.getServletResponse().getStatus();
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.OK;
    }

    private String getRequestPath(ServerHttpRequest request) {
        return request instanceof ServletServerHttpRequest servletRequest
                ? servletRequest.getServletRequest().getRequestURI()
                : DEFAULT_PATH;
    }

    /**
     * Generates a context-specific message based on the method or response.
     */
    private String generateMessage(MethodParameter returnType, Object body) {
        var method = returnType.getMethod();
        if (method == null)
            return null;
        String name = method.getName().toLowerCase();

        if (name.contains("get") || name.contains("find")) {
            return body != null ? "Resource retrieved successfully" : null;
        }
        if (name.contains("save") || name.contains("create")) {
            return BaseConstants.DATA_SAVED_SUCCESSFULLY;
        }
        if (name.contains("update")) {
            return BaseConstants.DATA_UPDATED_SUCCESSFULLY;
        }
        if (name.contains("delete")) {
            return BaseConstants.DATA_DELETED_SUCCESSFULLY;
        }
        if (name.contains("enable")) {
            return BaseConstants.DATA_ENABLED_SUCCESSFULLY;
        }
        if (name.contains("disable")) {
            return BaseConstants.DATA_DISABLED_SUCCESSFULLY;
        }
        return null;
    }

    /**
     * Handles unexpected exceptions to return a consistent CustomResponse.
     */
    @ExceptionHandler(Exception.class)
    public CustomResponse<Object> handleException(Exception ex, WebRequest request) {
        String path = request != null ? request.getContextPath() : "";
        return CustomResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), path);
    }
}
