package com.erp.platform.events;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;

/**
 * Interface for consuming events.
 *
 * <p>Provides methods for handling domain and integration events
 * from the event bus.
 *
 * @param <T> the event payload type
 * @since 1.0.0
 */
public interface EventConsumer<T> {

    /**
     * Handles a domain event.
     *
     * @param event the domain event to handle
     */
    void handle(DomainEvent<?> event);

    /**
     * Handles an integration event.
     *
     * @param event the integration event to handle
     */
    void handle(IntegrationEvent<?> event);

    /**
     * Gets the event types this consumer handles.
     *
     * @return the event types
     */
    String[] getEventTypes();

    /**
     * Gets the consumer group name.
     *
     * @return the consumer group name
     */
    String getConsumerGroup();
}
