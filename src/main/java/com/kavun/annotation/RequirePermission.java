package com.kavun.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to check if user has permission for a specific action on a page.
 *
 * Permission check logic:
 * 1. If pageActions is specified: Check if user has any of the specified page:action combinations
 * 2. Otherwise: Auto-determine action from HTTP method (GET->VIEW, POST->CREATE, etc.) and check against endpoint
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * List of allowed page:action combinations for this endpoint.
     * Format: "PAGE_CODE:ACTION_CODE"
     *
     * Example:
     * pageActions = {
     *     "USER_LIST:VIEW",
     *     "REPORTS:EXPORT",
     *     "DASHBOARD:WIDGET_VIEW"
     * }
     *
     * User needs ANY ONE of these combinations to access the endpoint (OR logic).
     * If empty, will auto-determine action from HTTP method and check against request endpoint.
     */
    String[] pageActions() default {};

    /**
     * Custom error message if permission is denied
     */
    String message() default "You do not have permission to perform this action.";
}
