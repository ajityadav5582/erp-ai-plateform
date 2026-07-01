package com.erp.platform.events.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for domain events.
 *
 * <p>Provides standard metadata fields that are included with all events.
 *
 * @since 1.0.0
 */
public record EventMetadata(
    String eventId,
    String tenantId,
    Instant occurredAt,
    String version,
    String eventName,
    Map<String, Object> attributes
) implements Serializable {

    /**
     * Creates event metadata with minimal information.
     */
    public EventMetadata(String eventId, String tenantId, String eventName, String version) {
        this(eventId, tenantId, Instant.now(), version, eventName, new HashMap<>());
    }

    /**
     * Creates event metadata with all fields.
     */
    public EventMetadata(String eventId, String tenantId, Instant occurredAt,
                         String version, String eventName, Map<String, Object> attributes) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.occurredAt = occurredAt;
        this.version = version;
        this.eventName = eventName;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
    }

    /**
     * Adds an attribute to the metadata.
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return a new metadata instance with the added attribute
     */
    public EventMetadata withAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.attributes);
        newAttributes.put(key, value);
        return new EventMetadata(eventId, tenantId, occurredAt, version, eventName, newAttributes);
    }

    /**
     * Gets an attribute from the metadata.
     *
     * @param key the attribute key
     * @param <T> the attribute type
     * @return the attribute value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}
