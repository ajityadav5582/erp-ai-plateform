package com.erp.platform.events;

/**
 * Exception thrown when event serialization or deserialization fails.
 *
 * @since 1.0.0
 */
public class EventSerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new event serialization exception.
     *
     * @param message the error message
     */
    public EventSerializationException(String message) {
        super(message);
    }

    /**
     * Creates a new event serialization exception.
     *
     * @param message the error message
     * @param cause the root cause
     */
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
