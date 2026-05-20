package com.glazowr.usage_service.service;

import com.glazowr.usage_service.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboxCleanupService {

    private final InboxRepository inboxRepository;

    @Value("${inbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "${inbox.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void cleanupOldInboxEntries() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting inbox cleanup: removing entries before {}", cutoff);

        try {
            int deleted = inboxRepository.deleteOldEntries(cutoff);
            log.info("Inbox cleanup completed: {} records deleted", deleted);
        } catch (Exception e) {
            log.error("Inbox cleanup failed", e);
        }
    }
}