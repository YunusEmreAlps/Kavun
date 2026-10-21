package com.kavun.shared.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Utility class to provide access to Spring ApplicationContext from static methods.
 * This is useful for utility classes that cannot use dependency injection.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.context = applicationContext;
    }

    /**
     * Get a bean from the application context.
     *
     * @param beanClass the class of the bean
     * @param <T> the type of the bean
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        return context.getBean(beanClass);
    }

    /**
     * Get the application context.
     *
     * @return the application context
     */
    public static ApplicationContext getContext() {
        return context;
    }
}
