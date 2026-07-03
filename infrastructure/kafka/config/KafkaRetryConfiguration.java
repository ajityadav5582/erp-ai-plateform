package com.erp.platform.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.ExponentialBackOff;

/**
 * Kafka retry and DLQ configuration.
 * Provides error handling with retry and dead letter queue support.
 */
@Configuration
public class KafkaRetryConfiguration {

    @Value("${kafka.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${kafka.retry.initial-delay:1000}")
    private long initialDelay;

    @Value("${kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${kafka.retry.max-delay:60000}")
    private long maxDelay;

    /**
     * Configure error handler with retry and DLQ support.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Create DLQ recoverer
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(
                record.topic() + ".dlq",
                record.partition()
            )
        );

        // Configure exponential backoff
        ExponentialBackOff backOff = new ExponentialBackOff(initialDelay, multiplier);
        backOff.setMaxInterval(maxDelay);

        return new DefaultErrorHandler(
            recoverer,
            backOff
        );
    }

    /**
     * Configure Kafka listener container factory with retry support.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory(
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory factory =
            new ConcurrentKafkaListenerContainerFactory();

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
