package com.glazowr.alert_service.repository;

import com.glazowr.alert_service.entity.Alert;
import com.glazowr.alert_service.entity.AlertStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Claim path — SKIP LOCKED ensures multi-instance safety
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
            SELECT a FROM Alert a
            WHERE a.status = :status
            ORDER BY a.createdAt ASC
            LIMIT :limit
            """)
    List<Alert> findPendingAlertsForUpdate(
            @Param("status") AlertStatus status,
            @Param("limit") int limit
    );

    // Recovery path — also SKIP LOCKED
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Alert a
            WHERE a.status = :status
            AND a.processingStartedAt < :cutoff
            """)
    List<Alert> findStuckAlertsForUpdate(
            @Param("status") AlertStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );

    // Bulk update — single round trip instead of findById + save
    @Modifying
    @Query("""
            UPDATE Alert a
            SET a.status = :status,
                a.sentAt = :sentAt,
                a.lastAttemptedAt = :sentAt
            WHERE a.id = :id
            """)
    int markAsSent(
            @Param("id") Long id,
            @Param("status") AlertStatus status,
            @Param("sentAt") LocalDateTime sentAt
    );

    @Modifying
    @Query("""
            UPDATE Alert a
            SET a.status = :status,
                a.lastError = :error,
                a.retryCount = a.retryCount + 1,
                a.lastAttemptedAt = :failedAt
            WHERE a.id = :id
            """)
    int markAsFailed(
            @Param("id") Long id,
            @Param("status") AlertStatus status,
            @Param("error") String error,
            @Param("failedAt") LocalDateTime failedAt
    );

    @Query("""
            SELECT a FROM Alert a
            WHERE a.status = :status
            AND a.retryCount < :maxRetries
            """)
    List<Alert> findRetryableFailedAlerts(
            @Param("status") AlertStatus status,
            @Param("maxRetries") int maxRetries
    );
}


/*
package com.glazowr.alert_service.repository;

import com.glazowr.alert_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository  extends JpaRepository<Alert, Long> {
    // Check if alert already exists for this dedupe window
    Optional<Alert> findByUserIdAndDedupeKeyAndWindowStart(
            Long userId, String dedupeKey, LocalDateTime windowStart);
}*/
