package com.erp.business.inventory.infrastructure.messaging;

import com.erp.platform.events.EventPublisher;
import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-op {@link EventPublisher} fallback used when Kafka is not configured.
 *
 * <p>Only registered when no {@link KafkaTemplate} bean is present (i.e. when
 * {@link KafkaEventPublisher} is not created). All publish methods are no-ops,
 * so callers such as {@code UnitServiceImpl} can create/update units without
 * Kafka being available. Events are silently dropped in this mode.
 */
@Component
@ConditionalOnMissingBean(KafkaTemplate.class)
public class NoOpEventPublisher implements EventPublisher<Object> {

    private static final Logger log = LoggerFactory.getLogger(NoOpEventPublisher.class);

    @Override
    public void publish(DomainEvent<?> event) {
        log.debug("Dropping domain event (no KafkaTemplate configured): {}", event.getEventName());
    }

    @Override
    public void publish(IntegrationEvent<?> event) {
        log.debug("Dropping integration event (no KafkaTemplate configured): {}", event.getEventName());
    }

    @Override
    public void publishAsync(DomainEvent<?> event) {
        log.debug("Dropping async domain event (no KafkaTemplate configured): {}", event.getEventName());
    }

    @Override
    public void publishAsync(IntegrationEvent<?> event) {
        log.debug("Dropping async integration event (no KafkaTemplate configured): {}", event.getEventName());
    }
}
