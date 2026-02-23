package com.kavun.constant.base;

import com.kavun.constant.ErrorConstants;

/**
 * This class holds all constants used by the operations available to the BASE.
 *
 * @version 1.0
 * @since 1.0
 */
public final class BaseConstants {
    // General
    public static final String OPERATION_SUCCESSFUL = "Operation completed successfully";
    public static final String RESOURCE_CREATED = "Resource created successfully";
    public static final String RESOURCE_UPDATED = "Resource updated successfully";
    public static final String RESOURCE_DELETED = "Resource deleted successfully";
    public static final String RESOURCE_RETRIEVED = "Resource retrieved successfully";
    public static final String RESOURCES_RETRIEVED = "Resources retrieved successfully";
    public static final String NO_CONTENT = "Operation completed with no content";
    public static final String INVALID_REQUEST_PARAMETERS = "Invalid request parameters";
    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String RESOURCE_CONFLICT = "Resource conflict detected";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String RESOURCE_ALREADY_EXISTS = "Resource already exists";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";
    public static final String SERVICE_UNAVAILABLE = "Service temporarily unavailable";

    public static final String DATA_INTEGRITY_VIOLATION = "Data integrity violation. Please check your request data.";
    public static final String INVALID_JSON_REQUEST = "Invalid JSON request. Please check your request body.";
    public static final String RESOURCE_NOT_FOUND = "The requested resource was not found.";
    public static final String AUTHENTICATION_FAILED = "Authentication failed: ";
    public static final String INVALID_CREDENTIALS = "Invalid username or password";
    public static final String VALUE_ALREADY_EXISTS = "A record with this value already exists";
    public static final String VALUE_REFERENCED = "Cannot delete record - it is referenced by other records";
    public static final String INSUFFICIENT_PERMISSIONS = "You do not have the necessary permissions for this operation.";
    public static final String DATABASE_ERROR = "A database error occurred. Please try again later.";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please try again later.";

    private BaseConstants() {
        throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
    }
}
