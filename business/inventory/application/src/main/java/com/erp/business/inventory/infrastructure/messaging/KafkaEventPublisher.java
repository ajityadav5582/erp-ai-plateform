package com.erp.business.inventory.infrastructure.messaging;

import com.erp.platform.events.EventPublisher;
import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Kafka-based implementation of the {@link EventPublisher} interface.
 *
 * <p>Publishes domain and integration events to Kafka topics for cross-service
 * communication. Events are serialized as JSON and published to a topic named
 * after the event type.
 *
 * <p>Only registered when a {@link KafkaTemplate} bean is present; when Kafka is
 * not configured, the {@link NoOpEventPublisher} fallback is used instead so that
 * unit creation/update (and other event-publishing callers) do not fail.
 *
 * <p>All publish methods catch and log exceptions rather than throwing them, so a
 * down or unreachable Kafka broker never breaks the calling operation.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaEventPublisher implements EventPublisher<Object> {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void publish(DomainEvent<?> event) {
        try {
            String topic = event.getEventName();
            kafkaTemplate.send(topic, event.getEventId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish domain event {} to Kafka: {}", event.getEventName(), ex.getMessage());
        }
    }

    @Override
    public void publish(IntegrationEvent<?> event) {
        try {
            String topic = event.getEventName();
            kafkaTemplate.send(topic, event.getEventId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish integration event {} to Kafka: {}", event.getEventName(), ex.getMessage());
        }
    }

    @Override
    public void publishAsync(DomainEvent<?> event) {
        try {
            String topic = event.getEventName();
            kafkaTemplate.send(topic, event.getEventId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish async domain event {} to Kafka: {}", event.getEventName(), ex.getMessage());
        }
    }

    @Override
    public void publishAsync(IntegrationEvent<?> event) {
        try {
            String topic = event.getEventName();
            kafkaTemplate.send(topic, event.getEventId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish async integration event {} to Kafka: {}", event.getEventName(), ex.getMessage());
        }
    }
}
