package com.erp.platform.events.domain;

/**
 * Interface for publishing domain events.
 *
 * <p>Provides a contract for event publishing mechanisms.
 * Implementations can use Kafka, RabbitMQ, or any other messaging system.
 *
 * @since 1.0.0
 */
public interface EventPublisher {

    /**
     * Publishes a domain event.
     *
     * @param event the event to publish
     * @throws EventPublishException if the event cannot be published
     */
    void publish(DomainEvent<?> event) throws EventPublishException;

    /**
     * Publishes a domain event asynchronously.
     *
     * @param event the event to publish
     * @return a CompletableFuture that completes when the event is published
     */
    java.util.concurrent.CompletableFuture<Void> publishAsync(DomainEvent<?> event);

    /**
     * Exception thrown when an event cannot be published.
     */
    class EventPublishException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EventPublishException(String message) {
            super(message);
        }

        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
