package com.erp.platform.events;

/**
 * Defines naming conventions for events.
 *
 * <p>Provides constants and utilities for consistent event naming
 * across the platform.
 *
 * @since 1.0.0
 */
public final class EventNamingConvention {

    /**
     * Prefix for domain events.
     */
    public static final String DOMAIN_EVENT_PREFIX = "domain.";

    /**
     * Prefix for integration events.
     */
    public static final String INTEGRATION_EVENT_PREFIX = "integration.";

    /**
     * Separator for event name components.
     */
    public static final String SEPARATOR = ".";

    private EventNamingConvention() {
        // Utility class
    }

    /**
     * Builds a domain event name.
     *
     * @param aggregate the aggregate name
     * @param eventType the event type
     * @return the formatted event name
     */
    public static String domainEvent(String aggregate, String eventType) {
        return DOMAIN_EVENT_PREFIX + aggregate + SEPARATOR + eventType;
    }

    /**
     * Builds an integration event name.
     *
     * @param source the event source
     * @param eventType the event type
     * @return the formatted event name
     */
    public static String integrationEvent(String source, String eventType) {
        return INTEGRATION_EVENT_PREFIX + source + SEPARATOR + eventType;
    }
}
