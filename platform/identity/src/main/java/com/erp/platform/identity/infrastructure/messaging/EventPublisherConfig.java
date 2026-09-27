package com.erp.platform.identity.infrastructure.messaging;

import com.erp.platform.events.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the {@link EventPublisher} infrastructure bean.
 *
 * <p>Provides a Kafka-backed {@link EventPublisher} implementation. The
 * {@link KafkaTemplate} is auto-configured by Spring Boot from the
 * {@code spring.kafka.*} properties; it is intentionally lazy so the
 * application context starts even when no Kafka broker is reachable.
 *
 * @since 1.0.0
 */
@Configuration
public class EventPublisherConfig {

    @Bean
    public EventPublisher<com.erp.platform.events.domain.DomainEvent<?>> eventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher(kafkaTemplate);
    }
}
