package com.glazowr.alert_service.service;

import com.glazowr.kafka.event.AlertingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertService {

    private final EmailService emailService;

    public AlertService(EmailService emailService) {
        this.emailService = emailService;
    }


    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0
            ),
            dltTopicSuffix = ".DLT",
            autoCreateTopics = "true"
    )
    @Transactional
    @KafkaListener(topics = "energy-alerts", groupId = "alert-service")
    public void energyUsageAlertEvent(AlertingEvent alertingEvent, @Header(KafkaHeaders.DELIVERY_ATTEMPT) Integer deliveryAttempt) {
        log.info(
                "Received alert event={} attempt={}",
                alertingEvent,
                deliveryAttempt
        );

        try {

            String dedupeKey =
                    buildDedupeKey(
                            "ENERGY_THRESHOLD",
                            event.threshold());

            Alert alert = Alert.builder()
                    .userId(event.userId())
                    .email(event.email())
                    .message(event.message())
                    .threshold(event.threshold())
                    .energyConsumed(event.energyConsumed())
                    .windowStart(
                            LocalDateTime.ofInstant(
                                    event.windowStart(),
                                    ZoneOffset.UTC))
                    .dedupeKey(dedupeKey)
                    .status(AlertStatus.PENDING)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            alertRepository.saveAndFlush(alert);

            log.info(
                    "Alert job created for user={}",
                    event.userId());

        } catch (DataIntegrityViolationException e) {

            log.info(
                    "Duplicate alert skipped user={}",
                    event.userId());
        }

/*        // send email alert
        final String subject = "Energy Usage Alert for User "
                + alertingEvent.getUserId();
        final String message = "Alert: " + alertingEvent.getMessage() +
                "\nThreshold: " + alertingEvent.getThreshold() +
                "\nEnergy Consumed: " + alertingEvent.getEnergyConsumed();
        emailService.sendEmail(alertingEvent.getEmail(),
                subject,
                message,
                alertingEvent.getUserId());*/


    }

    private String buildDedupeKey(
            String type,
            Double threshold
    ) {
        return String.format(
                "%s:%.2f",
                type,
                threshold
        );
    }

    @DltHandler
    public void dltHandler(
            AlertingEvent event,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE)
            String exceptionMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC)
            String topic
    ) {

        log.error(
                """
                        ALERT EVENT MOVED TO DLT
                        topic={}
                        event={}
                        error={}
                        """,
                topic,
                event,
                exceptionMessage
        );
    }
}
