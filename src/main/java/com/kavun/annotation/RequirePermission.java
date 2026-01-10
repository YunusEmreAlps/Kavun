package com.kavun.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to check if user has permission for a specific action on a page.
 * Uses the HTTP method and endpoint to determine the required permission.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String endpoint() default "";

    String method() default "";

    String message() default "Bu işlemi gerçekleştirmek için izniniz bulunmamaktadır.";
}
