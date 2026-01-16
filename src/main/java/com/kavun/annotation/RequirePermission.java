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
     * If empty and autoDetect=true, will auto-determine action from HTTP method and page from header/path.
     */
    String[] pageActions() default {};

    /**
     * If true and pageActions is empty, automatically determines page:action from:
     * 1. Page-Code header (frontend gönderir)
     * 2. URL path (/api/v1/action -> ACTION page)
     * 3. HTTP method (GET->VIEW, POST->CREATE, PUT->EDIT, DELETE->DELETE)
     */
    boolean autoDetect() default false;

    /**
     * Override the action part when using autoDetect.
     * Useful when HTTP method doesn't match the actual action.
     * 
     * Example: POST request but needs APPROVE permission:
     * @RequirePermission(autoDetect = true, actionOverride = "APPROVE")
     * Result: PAGE_CODE:APPROVE instead of PAGE_CODE:CREATE
     */
    String actionOverride() default "";

    /**
     * Custom error message if permission is denied
     */
    String message() default "Bu işlemi gerçekleştirmek için izniniz bulunmamaktadır.";
}
