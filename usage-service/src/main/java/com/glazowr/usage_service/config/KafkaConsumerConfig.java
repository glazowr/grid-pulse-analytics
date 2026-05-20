package com.glazowr.usage_service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOffWithMaxRetries;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> {

                            log.error(
                                    "Routing message to DLT topic={} key={} error={}",
                                    record.topic(),
                                    record.key(),
                                    ex.getMessage()
                            );

                            return new TopicPartition(
                                    record.topic() + ".DLT",
                                    record.partition()
                            );
                        }
                );

        ExponentialBackOffWithMaxRetries backoff =
                new ExponentialBackOffWithMaxRetries(3);

        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, backoff);

        // non-retryable poison payloads
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                ClassCastException.class
        );

        return errorHandler;
    }

    @Bean
    public NewTopic energyUsageDLTTopic() {
        return TopicBuilder.name("energy-usage.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}