package com.kavun.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an API endpoint (or every endpoint of a controller) as open to any authenticated user,
 * without a page permission. Use it only for endpoints that act on the caller's own data, such as
 * changing the own password or loading the own navigation menu.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedEndpoint {}
