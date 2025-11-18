package com.kavun.web.payload.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standardized response codes for API responses.
 * Follows enterprise best practices with unique codes for each operation.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
public enum ResponseCode {
    // Success codes (2xx)
    SUCCESS("SUCCESS", "Operation completed successfully", HttpStatus.OK),
    CREATED("CREATED", "Resource created successfully", HttpStatus.CREATED),
    UPDATED("UPDATED", "Resource updated successfully", HttpStatus.OK),
    DELETED("DELETED", "Resource deleted successfully", HttpStatus.OK),
    RETRIEVED("RETRIEVED", "Resource retrieved successfully", HttpStatus.OK),
    NO_CONTENT("NO_CONTENT", "Operation completed with no content", HttpStatus.NO_CONTENT),

    // Client error codes (4xx)
    BAD_REQUEST("BAD_REQUEST", "Invalid request parameters", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Access denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "Resource conflict detected", HttpStatus.CONFLICT),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST),
    ALREADY_EXISTS("ALREADY_EXISTS", "Resource already exists", HttpStatus.CONFLICT),

    // Server error codes (5xx)
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("DATABASE_ERROR", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ResponseCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * Get the HTTP status code value.
     */
    public int getStatusValue() {
        return httpStatus.value();
    }
}
