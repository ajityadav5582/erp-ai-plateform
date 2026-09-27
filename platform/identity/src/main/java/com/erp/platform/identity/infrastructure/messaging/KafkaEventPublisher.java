package com.erp.platform.identity.infrastructure.messaging;

import com.erp.platform.events.EventPublisher;
import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka-backed implementation of {@link EventPublisher}.
 *
 * <p>Publishes domain and integration events to Kafka topics derived from the
 * event name. Publishing failures are logged and swallowed so that the
 * surrounding business transaction is never aborted because the event bus is
 * temporarily unavailable (e.g. during local development without a broker).
 *
 * @since 1.0.0
 */
public class KafkaEventPublisher implements EventPublisher<DomainEvent<?>> {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private static final String DEFAULT_TOPIC = "identity-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent<?> event) {
        send(event, resolveTopic(event.getEventName()));
    }

    @Override
    public void publish(IntegrationEvent<?> event) {
        send(event, resolveTopic(event.getEventName()));
    }

    @Override
    public void publishAsync(DomainEvent<?> event) {
        sendAsync(event, resolveTopic(event.getEventName()));
    }

    @Override
    public void publishAsync(IntegrationEvent<?> event) {
        sendAsync(event, resolveTopic(event.getEventName()));
    }

    private void send(Object event, String topic) {
        try {
            kafkaTemplate.send(new ProducerRecord<>(topic, null, event)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing event to topic '{}': {}", topic, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to publish event to topic '{}': {}", topic, e.getMessage());
        }
    }

    private void sendAsync(Object event, String topic) {
        try {
            kafkaTemplate.send(new ProducerRecord<>(topic, null, event));
        } catch (Exception e) {
            log.warn("Failed to publish async event to topic '{}': {}", topic, e.getMessage());
        }
    }

    private String resolveTopic(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return DEFAULT_TOPIC;
        }
        return eventName;
    }
}
