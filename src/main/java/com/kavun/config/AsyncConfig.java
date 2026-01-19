package com.kavun.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.spi.MDCAdapter;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Asynchronous task execution configuration.
 * Optimized for email sending with proper thread pool management.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - threads always alive
        executor.setCorePoolSize(5);

        // Maximum pool size - scales up during high load
        executor.setMaxPoolSize(20);

        // Queue capacity - emails waiting for processing
        executor.setQueueCapacity(200);

        // Thread naming for better logging/debugging
        executor.setThreadNamePrefix("Email-Async-");

        // Rejection policy - fallback to caller thread if queue full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Graceful shutdown configuration
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // Wait up to 2 minutes

        // Thread lifecycle optimization
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(300); // 5 minutes idle timeout

        // Task decorator for MDC context propagation
        executor.setTaskDecorator(new LoggingTaskDecorator());

        // Thread pool monitoring
        executor.setThreadFactory(r -> {
            Thread thread = new Thread(r);
            thread.setUncaughtExceptionHandler((t, e) ->
                LOG.error("Uncaught exception in thread {}: {}", t.getName(), e.getMessage(), e)
            );
            return thread;
        });

        executor.initialize();

        LOG.info("Initialized emailTaskExecutor - Core: {}, Max: {}, Queue: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * General purpose async executor for non-email tasks.
     * Smaller pool size for general operations.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-Task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.initialize();

        LOG.info("Initialized general taskExecutor - Core: {}, Max: {}, Queue: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            LOG.error("Async method '{}' threw uncaught exception: {}",
                method.getName(), throwable.getMessage(), throwable);
            LOG.error("Method parameters: {}", objects);
        };
    }

    /**
     * Task decorator for MDC (Mapped Diagnostic Context) propagation.
     * Ensures logging context is preserved across async threads.
     */
    private static class LoggingTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Copy MDC context from parent thread
            MDCAdapter mdcContext = org.slf4j.MDC.getMDCAdapter();
            return () -> {
                try {
                    runnable.run();
                } finally {
                    // Clear MDC after task completion
                    org.slf4j.MDC.clear();
                }
            };
        }
    }
}
