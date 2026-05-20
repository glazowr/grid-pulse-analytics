package com.glazowr.usage_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inbox",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "event_type"}, name = "uk_event_id_type"))
public class InboxEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String eventType;  // "ENERGY_USAGE" | "ALERT"

    @Column(nullable = false)
    private Instant processedAt;

    @Column(length = 64)
    private String payloadHash;

    public static InboxEntry forEnergyUsage(String eventId) {
        return InboxEntry.builder().eventId(eventId).eventType("ENERGY_USAGE")
                .processedAt(Instant.now()).build();
    }

    public static InboxEntry forAlert(String eventId) {
        return InboxEntry.builder().eventId(eventId).eventType("ALERT")
                .processedAt(Instant.now()).build();
    }
}