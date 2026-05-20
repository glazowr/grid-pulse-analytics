package com.glazowr.ingestion_service.service;

import com.glazowr.ingestion_service.dto.EnergyUsageDto;
import com.glazowr.kafka.event.EnergyUsageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class IngestionService {

    private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

    public IngestionService(KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void ingestEnergyUsage(EnergyUsageDto input) {

        // Generate eventId if not provided (backward compatible)
        final String eventId = input.eventId() != null ? input.eventId() : UUID.randomUUID().toString();

        // Convert DTO to Event
        EnergyUsageEvent event = EnergyUsageEvent.builder()
                .eventId(eventId)
                .deviceId(input.deviceId())
                .energyConsumed(input.energyConsumed())
                .timestamp(input.timestamp())
                .build();

        // Send to Kafka Topic
        // all events for same deviceId hash to same partition, ordering preserved PER DEVICE
        kafkaTemplate.send("energy-usage", String.valueOf(event.deviceId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event {}: {}", eventId, ex.getMessage());
                    }
                    else {
                        log.info(
                                "Sent eventId={} to partition={}, offset={}",
                                eventId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()
                        );
                    }
                });
    }
}
