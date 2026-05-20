package com.glazowr.kafka.event;

import lombok.Builder;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public record AlertingEvent(
        Long userId,
        String message,
        double threshold,
        double energyConsumed,
        String email,
        Instant windowStart
) {
}



/*package com.glazowr.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertingEvent {
    String eventId,
    private Long userId;
    private String message;
    private double threshold;
    private double energyConsumed;
    private String email;
}*/
