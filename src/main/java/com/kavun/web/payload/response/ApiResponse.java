package com.kavun.web.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * API response wrapper.
 * Provides consistent structure for all API responses.
 *
 * @param <T> the type of the response data
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Timestamp of the response */
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    /** HTTP status code */
    private int status;

    /** Response code for client identification */
    private String code;

    /** Human-readable message */
    private String message;

    /** Response data payload */
    private T data;

    /** Request path that generated this response */
    private String path;

    /** Validation errors (only for validation failures) */
    private Map<String, List<String>> errors;

    /** Additional metadata (optional) */
    private Map<String, Object> metadata;

    /**
     * Create a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code(ResponseCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful response with specific response code.
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data, String path) {
        return ApiResponse.<T>builder()
                .status(responseCode.getStatusValue())
                .code(responseCode.getCode())
                .message(responseCode.getMessage())
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful response without data.
     */
    public static <T> ApiResponse<T> success(String message, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .code(ResponseCode.SUCCESS.getCode())
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message, String path) {
        return ApiResponse.<T>builder()
                .status(responseCode.getStatusValue())
                .code(responseCode.getCode())
                .message(message != null ? message : responseCode.getMessage())
                .path(path)
                .build();
    }

    /**
     * Create an error response with validation errors.
     */
    public static <T> ApiResponse<T> validationError(Map<String, List<String>> errors, String path) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ResponseCode.VALIDATION_ERROR.getCode())
                .message(ResponseCode.VALIDATION_ERROR.getMessage())
                .errors(errors)
                .path(path)
                .build();
    }

    /**
     * Create an error response from HttpStatus.
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message, String path) {
        ResponseCode responseCode = mapHttpStatusToResponseCode(status);
        return ApiResponse.<T>builder()
                .status(status.value())
                .code(responseCode.getCode())
                .message(message != null ? message : responseCode.getMessage())
                .path(path)
                .build();
    }

    /**
     * Convert to ResponseEntity.
     */
    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }

    /**
     * Map HttpStatus to ResponseCode.
     */
    private static ResponseCode mapHttpStatusToResponseCode(HttpStatus status) {
        return switch (status) {
            case OK -> ResponseCode.SUCCESS;
            case CREATED -> ResponseCode.CREATED;
            case NO_CONTENT -> ResponseCode.NO_CONTENT;
            case BAD_REQUEST -> ResponseCode.BAD_REQUEST;
            case UNAUTHORIZED -> ResponseCode.UNAUTHORIZED;
            case FORBIDDEN -> ResponseCode.FORBIDDEN;
            case NOT_FOUND -> ResponseCode.NOT_FOUND;
            case CONFLICT -> ResponseCode.CONFLICT;
            case INTERNAL_SERVER_ERROR -> ResponseCode.INTERNAL_ERROR;
            case SERVICE_UNAVAILABLE -> ResponseCode.SERVICE_UNAVAILABLE;
            default -> ResponseCode.INTERNAL_ERROR;
        };
    }
}
