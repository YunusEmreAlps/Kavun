package com.kavun.constant.base;

import com.kavun.constant.ErrorConstants;

/**
 * This class holds all constants used by the operations available to the BASE.
 *
 * @version 1.0
 * @since 1.0
 */
public final class BaseConstants {

    // Field sizes

    // Validation messages
    public static final String PUBLIC_ID_NOT_BLANK = "Public ID must not be blank";
    public static final String DATA_EXIST = "Data exists with publicId: {0}";
    public static final String DATA_NOT_FOUND = "Data not found";
    public static final String DATA_FOUND = "Data found with publicId: {0}";
    public static final String DATA_SAVED_SUCCESSFULLY = "Data saved successfully";
    public static final String DATA_UPDATED_SUCCESSFULLY = "Data updated successfully";
    public static final String DATA_DELETED_SUCCESSFULLY = "Data deleted successfully";
    public static final String DATA_SOFT_DELETED_SUCCESSFULLY = "Data soft deleted successfully";
    public static final String DATA_RECOVERED_SUCCESSFULLY = "Data recovered successfully";
    public static final String DATA_ALREADY_EXISTS = "Data already exists";
    public static final String DATA_CREATE_CONFLICT = "Data creation conflict with existing data";
    public static final String DATA_UPDATE_CONFLICT = "Data update conflict with existing data";
    public static final String DATA_ENABLED_SUCCESSFULLY = "Data enabled successfully";
    public static final String DATA_DISABLED_SUCCESSFULLY = "Data disabled successfully";

    private BaseConstants() {
        throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
    }
}
