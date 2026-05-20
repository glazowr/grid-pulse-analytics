package com.glazowr.alert_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.Positive;

@Data
@Component
@ConfigurationProperties(prefix = "alert-service")
public class AlertServiceProperties {

    @Positive
    private int batchSize = 50;

    @Positive
    private int multiThreadThreshold = 10;

    @Positive
    private int maxWorkers = 8;

    @Positive
    private int stuckTimeoutMinutes = 10;

    @Positive
    private int retryMaxAttempts = 5;

    private long schedulerFixedDelayMs = 5000;
    private long recoverySchedulerDelayMs = 60000;
    private long retrySchedulerDelayMs = 300000;
}