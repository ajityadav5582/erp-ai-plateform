package com.erp.business.inventory.config;

import com.erp.business.inventory.infrastructure.messaging.KafkaEventPublisher;
import com.erp.platform.events.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the {@link EventPublisher} infrastructure bean.
 */
@Configuration
public class EventPublisherConfig {

    @Bean
    public EventPublisher<Object> eventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher(kafkaTemplate);
    }
}
