package com.erp.platform.events.serializer;

import com.erp.platform.events.domain.DomainEvent;
import com.erp.platform.events.integration.IntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson-based event serializer.
 *
 * <p>Provides JSON serialization and deserialization of events
 * using Jackson.
 *
 * @since 1.0.0
 */
public class JacksonEventSerializer implements EventSerializer<Object> {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new Jackson event serializer.
     */
    public JacksonEventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public byte[] serialize(Object event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }

    @Override
    public Object deserialize(byte[] data, Class<?> eventType) {
        try {
            return objectMapper.readValue(data, eventType);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to deserialize event", e);
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    /**
     * Serializes a domain event.
     *
     * @param event the domain event to serialize
     * @return the serialized bytes
     */
    public byte[] serializeDomainEvent(DomainEvent<?> event) {
        return serialize(event);
    }

    /**
     * Serializes an integration event.
     *
     * @param event the integration event to serialize
     * @return the serialized bytes
     */
    public byte[] serializeIntegrationEvent(IntegrationEvent<?> event) {
        return serialize(event);
    }
}
