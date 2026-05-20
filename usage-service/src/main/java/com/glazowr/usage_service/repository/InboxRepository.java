package com.glazowr.usage_service.repository;

import com.glazowr.usage_service.model.InboxEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface InboxRepository extends JpaRepository<InboxEntry, Long> {

    boolean existsByEventIdAndEventType(String eventId, String eventType);

    @Modifying
    @Query("DELETE FROM InboxEntry e WHERE e.processedAt < :cutoff")
    int deleteOldEntries(@Param("cutoff") Instant cutoff);
}