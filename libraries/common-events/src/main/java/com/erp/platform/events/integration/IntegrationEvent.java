package com.erp.platform.events.integration;

import com.erp.platform.events.domain.DomainEvent;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for integration events.
 *
 * <p>Integration events are used for cross-service communication
 * and are typically published to a message broker.
 *
 * @since 1.0.0
 */
public interface IntegrationEvent<T> extends DomainEvent<T>, Serializable {

    /**
     * Returns the source service that published this event.
     *
     * @return the source service name
     */
    String getSourceService();

    /**
     * Returns the target service(s) for this event.
     *
     * @return the target service names
     */
    java.util.List<String> getTargetServices();

    /**
     * Returns the correlation ID for tracing.
     *
     * @return the correlation ID
     */
    String getCorrelationId();

    /**
     * Returns the causation ID (the event that caused this event).
     *
     * @return the causation ID
     */
    String getCausationId();

    /**
     * Returns the event payload.
     *
     * @return the event payload
     */
    Object getPayload();

    /**
     * Returns the event headers.
     *
     * @return the event headers
     */
    Map<String, Object> getHeaders();
}
