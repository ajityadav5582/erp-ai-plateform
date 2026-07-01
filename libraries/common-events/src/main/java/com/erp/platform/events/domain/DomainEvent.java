package com.erp.platform.events.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base interface for all domain events in the system.
 *
 * <p>Domain events represent something that happened in the domain
 * that other parts of the system might be interested in.
 *
 * @since 1.0.0
 */
public interface DomainEvent<T> extends Serializable {

    /**
     * Returns the unique event identifier.
     *
     * @return the event ID
     */
    UUID getEventId();

    /**
     * Returns the tenant ID associated with this event.
     *
     * @return the tenant ID
     */
    String getTenantId();

    /**
     * Returns the timestamp when the event occurred.
     *
     * @return the event timestamp
     */
    Instant getOccurredAt();

    /**
     * Returns the event version.
     *
     * @return the event version
     */
    String getVersion();

    /**
     * Returns the event name.
     *
     * @return the event name
     */
    String getEventName();

    /**
     * Returns additional metadata for the event.
     *
     * @return the event metadata
     */
    Map<String, Object> getMetadata();
}
