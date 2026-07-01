package com.erp.platform.events.domain;

/**
 * Interface for consuming domain events.
 *
 * <p>Provides a contract for event consumer mechanisms.
 * Implementations can use Kafka, RabbitMQ, or any other messaging system.
 *
 * @param <T> the type of event to consume
 * @since 1.0.0
 */
public interface EventConsumer<T extends DomainEvent<?>> {

    /**
     * Consumes a domain event.
     *
     * @param event the event to consume
     */
    void consume(T event);

    /**
     * Returns the event type this consumer handles.
     *
     * @return the event class
     */
    Class<T> getEventType();

    /**
     * Returns the consumer name for logging and monitoring.
     *
     * @return the consumer name
     */
    String getConsumerName();
}
