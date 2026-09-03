package com.kavun.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kavun.web.payload.response.ApiResponse;
import com.kavun.web.payload.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.RequestDispatcher;

import java.util.LinkedHashMap;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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
@RequiredArgsConstructor
@RestControllerAdvice
@Order(10)
public class ResponseHandler implements ResponseBodyAdvice<Object> {

  private static final String DEFAULT_PATH = "";

  /** Serializes the wrapper when the selected converter can only write a String. */
  private final ObjectMapper objectMapper;

  /** Swagger/OpenAPI path prefixes to exclude from response wrapping. */
  private static final String[] EXCLUDED_PATH_PREFIXES = {
      "/v3/api-docs", "/swagger", "/swagger-ui", "/actuator"
  };

  /** Swagger/OpenAPI path patterns to exclude from response wrapping. */
  private static final String[] EXCLUDED_PATH_PATTERNS = { "/api-docs" };

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request, ServerHttpResponse response) {

    String path = getRequestPath(request);

    // Controllers returning a plain String are served by StringHttpMessageConverter,
    // which can only write a String. Handing it an ApiResponse would fail with a
    // ClassCastException, so those wrappers are serialized here by hand.
    boolean stringConverter = StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType);

    // Error dispatches (Spring Security entry points, BasicErrorController, ...)
    // already describe the failure; wrapping their body would nest a second
    // error payload under "data".
    if (isErrorDispatch(request)) {
      ApiResponse<Object> errorResponse = ApiResponse.error(mapHttpStatusToResponseCode(getHttpStatus(response)),
          null, path);
      return renderWrapper(errorResponse, body, stringConverter, response);
    }

    // Exclude Swagger/OpenAPI endpoints from wrapping
    if (isSwaggerPath(path)) {
      return body;
    }

    // Only wrap JSON-like responses (application/json, application/hal+json, etc.).
    // String bodies are negotiated as text/plain by the converter itself, so they
    // are wrapped as well and the content type is corrected below.
    if (!stringConverter && !isJsonMediaType(selectedContentType)) {
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

      Object data = (body != null) ? body : new LinkedHashMap<>();
      return renderWrapper(ApiResponse.success(responseCode, data, path), body, stringConverter, response);
    } catch (Exception e) {
      LOG.warn("Failed to wrap response, returning original body: {}", e.getMessage());
      return body;
    }
  }

  /**
   * Hands the wrapper to the selected converter: JSON converters take the object
   * as-is, while StringHttpMessageConverter gets it pre-serialized with the
   * content type switched from text/plain to application/json.
   */
  private Object renderWrapper(ApiResponse<?> wrapper, Object originalBody, boolean stringConverter,
      ServerHttpResponse response) {
    if (!stringConverter) {
      return wrapper;
    }

    try {
      String json = objectMapper.writeValueAsString(wrapper);
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return json;
    } catch (JsonProcessingException e) {
      LOG.warn("Failed to serialize wrapped response, returning original body: {}", e.getMessage());
      return originalBody;
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

    var httpRequest = servletRequest.getServletRequest();

    // On an error dispatch the URI is /error; report the URI the client called.
    Object originalUri = httpRequest.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    if (originalUri instanceof String uri && !uri.isBlank()) {
      return uri;
    }

    String uri = httpRequest.getRequestURI();
    return uri != null ? uri : DEFAULT_PATH;
  }

  /**
   * Checks whether the response is produced by the servlet error dispatch.
   */
  private boolean isErrorDispatch(ServerHttpRequest request) {
    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      return false;
    }
    return servletRequest.getServletRequest().getAttribute(RequestDispatcher.ERROR_REQUEST_URI) != null;
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
