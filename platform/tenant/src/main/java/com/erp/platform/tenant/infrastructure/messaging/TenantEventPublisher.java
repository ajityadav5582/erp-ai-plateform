package com.erp.platform.tenant.infrastructure.messaging;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.tenant.domain.TenantCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Event publisher for tenant domain events.
 *
 * <p>Publishes tenant events to Kafka for consumption by other services.
 *
 * @since 1.0.0
 */
@Component
@Slf4j
public class TenantEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TenantEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventListener
    public void handle(TenantCreatedEvent event) {
        log.info("Publishing tenant created event: {}", event.tenantId());
        kafkaTemplate.send("tenant.created", event.tenantId().toString(), event);
    }
}
