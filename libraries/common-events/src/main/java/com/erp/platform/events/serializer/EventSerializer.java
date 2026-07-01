package com.erp.platform.events.serializer;

import com.erp.platform.events.domain.DomainEvent;

/**
 * Serializer for domain events.
 *
 * <p>Provides a contract for serializing and deserializing events.
 * Implementations can use JSON, Avro, Protobuf, or any other serialization format.
 *
 * @since 1.0.0
 */
public interface EventSerializer<T> {

    /**
     * Serializes a domain event to bytes.
     *
     * @param event the event to serialize
     * @return the serialized bytes
     * @throws EventSerializationException if serialization fails
     */
    byte[] serialize(T event) throws EventSerializationException;

    /**
     * Deserializes bytes to a domain event.
     *
     * @param data the serialized bytes
     * @param eventType the event type
     * @return the deserialized event
     * @throws EventSerializationException if deserialization fails
     */
    T deserialize(byte[] data, Class<? extends T> eventType) throws EventSerializationException;

    /**
     * Returns the content type for this serializer.
     *
     * @return the content type
     */
    String getContentType();

    /**
     * Exception thrown when serialization fails.
     */
    class EventSerializationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EventSerializationException(String message) {
            super(message);
        }

        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
