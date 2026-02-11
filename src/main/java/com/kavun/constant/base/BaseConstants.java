package com.kavun.constant.base;

import com.kavun.constant.ErrorConstants;

/**
 * This class holds all constants used by the operations available to the BASE.
 *
 * @version 1.0
 * @since 1.0
 */
public final class BaseConstants {

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
