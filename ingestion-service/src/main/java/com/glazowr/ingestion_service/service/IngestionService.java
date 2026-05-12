package com.glazowr.ingestion_service.service;

import com.glazowr.ingestion_service.dto.EnergyUsageDto;
import com.glazowr.kafka.event.EnergyUsageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionService {

    private final KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

    public IngestionService(KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void ingestEnergyUsage(EnergyUsageDto input) {
        // Convert DTO to Event
        EnergyUsageEvent event = EnergyUsageEvent.builder()
                .deviceId(input.deviceId())
                .energyConsumed(input.energyConsumed())
                .timestamp(input.timestamp())
                .build();

        // Send to Kafka Topic
        // all events for same deviceId hash to same partition, ordering preserved PER DEVICE
        kafkaTemplate.send(
                "energy-usage",
                String.valueOf(event.deviceId()),
                event
        );
        log.info("Ingested Energy Usage Event: {}", event);
    }
}
