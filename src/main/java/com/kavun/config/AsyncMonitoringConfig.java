package com.kavun.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AsyncMonitoringConfig {

    @Bean
    public ThreadPoolTaskExecutor monitoredUserTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MonitoredUser-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Add task decorator for monitoring
        executor.setTaskDecorator(runnable -> {
            return () -> {
                long startTime = System.currentTimeMillis();
                String threadName = Thread.currentThread().getName();
                LOG.info("TASK START [{}]: Starting async task", threadName);

                try {
                    runnable.run();
                    long duration = System.currentTimeMillis() - startTime;
                    LOG.info("TASK COMPLETE [{}]: Task completed in {}ms", threadName, duration);
                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    LOG.error("TASK ERROR [{}]: Task failed after {}ms", threadName, duration, e);
                    throw e;
                }
            };
        });

        executor.initialize();
        return executor;
    }
}
