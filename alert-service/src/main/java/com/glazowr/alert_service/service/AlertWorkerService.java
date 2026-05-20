package com.glazowr.alert_service.service;

import com.glazowr.alert_service.entity.Alert;
import com.glazowr.alert_service.entity.AlertStatus;
import com.glazowr.alert_service.repository.AlertRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertWorkerService {

    private final AlertRepository alertRepository;
    private final EmailService emailService;

    private static final int CLAIM_BATCH_SIZE = 50;
    private static final int MULTI_THREAD_THRESHOLD = 10;
    private static final int MAX_WORKERS = 8;
    private static final int STUCK_TIMEOUT_MINUTES = 10;
    private static final int MAX_RETRY_COUNT = 5;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            MAX_WORKERS,
            new NamedThreadFactory("alert-worker")
    );

    // -------------------------------------------------------------------------
    // MAIN SCHEDULER
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelay = 5000)
    public void processAlerts() {
        List<Alert> alerts = claimPendingAlerts();

        if (alerts.isEmpty()) {
            log.debug("No pending alerts found");
            return;
        }

        log.info("Claimed {} alerts for processing", alerts.size());

        if (alerts.size() < MULTI_THREAD_THRESHOLD) {
            processSequentially(alerts);
        } else {
            processConcurrently(alerts);
        }
    }

    // -------------------------------------------------------------------------
    // SEQUENTIAL
    // -------------------------------------------------------------------------

    private void processSequentially(List<Alert> alerts) {
        log.info("Processing alerts sequentially count={}", alerts.size());
        for (Alert alert : alerts) {
            processSingleAlert(alert);
        }
    }

    // -------------------------------------------------------------------------
    // CONCURRENT
    // -------------------------------------------------------------------------

    private void processConcurrently(List<Alert> alerts) {
        log.info("Processing alerts concurrently count={} workers={}", alerts.size(), MAX_WORKERS);

        List<CompletableFuture<Void>> futures = alerts.stream()
                .map(alert -> CompletableFuture.runAsync(
                        () -> processSingleAlert(alert),
                        executorService
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Concurrent alert processing completed");
    }

    // -------------------------------------------------------------------------
    // SINGLE ALERT PROCESSING
    // -------------------------------------------------------------------------

    private void processSingleAlert(Alert alert) {
        try {
            log.info("Sending email alertId={} userId={}", alert.getId(), alert.getUserId());

            emailService.sendEmail(
                    alert.getEmail(),
                    "Energy Usage Alert",
                    buildMessage(alert),
                    alert.getUserId()
            );

            markSent(alert.getId());
            log.info("Successfully sent alertId={}", alert.getId());

        } catch (Exception e) {
            log.error("Failed processing alertId={}", alert.getId(), e);
            markFailed(alert.getId(), e);
        }
    }

    // -------------------------------------------------------------------------
    // CLAIM PENDING ALERTS
    // Uses FOR UPDATE SKIP LOCKED — safe under multi-instance deployment.
    // Each instance claims a non-overlapping batch; no duplicate processing.
    // -------------------------------------------------------------------------

    @Transactional
    private List<Alert> claimPendingAlerts() {
        List<Alert> alerts = alertRepository.findPendingAlertsForUpdate(
                AlertStatus.PENDING,
                CLAIM_BATCH_SIZE
        );

        if (alerts.isEmpty()) return alerts;

        LocalDateTime now = LocalDateTime.now();
        for (Alert alert : alerts) {
            alert.setStatus(AlertStatus.PROCESSING);
            alert.setProcessingStartedAt(now);
        }

        log.info("Marked {} alerts as PROCESSING", alerts.size());
        return alerts;
    }

    // -------------------------------------------------------------------------
    // MARK SENT — bulk update, single DB round trip
    // -------------------------------------------------------------------------

    @Transactional
    private void markSent(Long id) {
        int updated = alertRepository.markAsSent(id, AlertStatus.SENT, LocalDateTime.now());
        if (updated == 0) {
            log.warn("markSent: no row updated for alertId={} — possible concurrent update", id);
        } else {
            log.info("Marked alertId={} as SENT", id);
        }
    }

    // -------------------------------------------------------------------------
    // MARK FAILED — bulk update, single DB round trip
    // -------------------------------------------------------------------------

    @Transactional
    private void markFailed(Long id, Exception e) {
        int updated = alertRepository.markAsFailed(
                id,
                AlertStatus.FAILED,
                e.getMessage(),
                LocalDateTime.now()
        );
        if (updated == 0) {
            log.warn("markFailed: no row updated for alertId={} — possible concurrent update", id);
        } else {
            log.warn("Marked alertId={} as FAILED", id);
        }
    }

    // -------------------------------------------------------------------------
    // RECOVER STUCK ALERTS
    // FOR UPDATE SKIP LOCKED prevents two instances from resetting the same row.
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void recoverStuckAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STUCK_TIMEOUT_MINUTES);

        List<Alert> stuckAlerts = alertRepository.findStuckAlertsForUpdate(
                AlertStatus.PROCESSING,
                cutoff
        );

        if (stuckAlerts.isEmpty()) return;

        log.warn("Recovering {} stuck alerts", stuckAlerts.size());

        for (Alert alert : stuckAlerts) {
            alert.setStatus(AlertStatus.PENDING);
            alert.setProcessingStartedAt(null);
        }
    }

    // -------------------------------------------------------------------------
    // RETRY FAILED ALERTS — exponential backoff per retryCount
    // retryCount=1 -> 2min, 2 -> 4min, 3 -> 8min, 4 -> 16min, 5 -> not retried
    // -------------------------------------------------------------------------

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryFailedAlerts() {
        LocalDateTime now = LocalDateTime.now();

        List<Alert> failedAlerts = alertRepository.findRetryableFailedAlerts(
                AlertStatus.FAILED,
                MAX_RETRY_COUNT
        );

        if (failedAlerts.isEmpty()) return;

        List<Alert> due = failedAlerts.stream()
                .filter(alert -> isRetryDue(alert, now))
                .toList();

        if (due.isEmpty()) return;

        log.info("Retrying {} failed alerts", due.size());
        due.forEach(alert -> alert.setStatus(AlertStatus.PENDING));
    }

    private boolean isRetryDue(Alert alert, LocalDateTime now) {
        if (alert.getLastAttemptedAt() == null) return true;
        long backoffMinutes = (long) Math.pow(2, alert.getRetryCount());
        return alert.getLastAttemptedAt().plusMinutes(backoffMinutes).isBefore(now);
    }

    // -------------------------------------------------------------------------
    // SHUTDOWN — prevents thread pool from blocking JVM shutdown
    // -------------------------------------------------------------------------

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down alert worker executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in 30s — forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String buildMessage(Alert alert) {
        return """
                Energy usage threshold exceeded.

                Threshold: %s
                Energy Consumed: %s
                """.formatted(alert.getThreshold(), alert.getEnergyConsumed());
    }

    // Named thread factory — threads show as alert-worker-1, alert-worker-2, etc.
    // Critical for reading thread dumps in production.
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}