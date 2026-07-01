package com.erp.platform.events;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;

/**
 * Interface for publishing events.
 *
 * <p>Provides methods for publishing domain and integration events
 * to the event bus.
 *
 * @param <T> the event payload type
 * @since 1.0.0
 */
public interface EventPublisher<T> {

    /**
     * Publishes a domain event.
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent<?> event);

    /**
     * Publishes an integration event.
     *
     * @param event the integration event to publish
     */
    void publish(IntegrationEvent<?> event);

    /**
     * Publishes a domain event asynchronously.
     *
     * @param event the domain event to publish
     */
    void publishAsync(DomainEvent<?> event);

    /**
     * Publishes an integration event asynchronously.
     *
     * @param event the integration event to publish
     */
    void publishAsync(IntegrationEvent<?> event);
}
