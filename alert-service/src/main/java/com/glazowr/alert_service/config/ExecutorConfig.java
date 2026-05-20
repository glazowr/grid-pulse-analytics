package com.glazowr.alert_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {

    private final AlertServiceProperties properties;

    public ExecutorConfig(AlertServiceProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "alertWorkerExecutor")
    public Executor alertWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getMaxWorkers());
        executor.setMaxPoolSize(properties.getMaxWorkers());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alert-worker-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}