package com.glazowr.alert_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "alert",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dedupe_window",
                columnNames = {
                        "user_id",
                        "dedupe_key",
                        "window_start"
                }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String email;

    @Column(length = 500)
    private String message;

    private Double threshold;

    private Double energyConsumed;

    private LocalDateTime createdAt;

    private LocalDateTime windowStart;

    private String dedupeKey;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private Integer retryCount;

    @Column(length = 2000)
    private String lastError;

    private LocalDateTime sentAt;

    private LocalDateTime processingStartedAt;
}
