package com.erp.platform.events.versioning;

/**
 * Event naming conventions.
 *
 * <p>Provides standard naming conventions for events
 * to ensure consistency across the platform.
 *
 * @since 1.0.0
 */
public final class EventNaming {

    private EventNaming() {
        // Utility class
    }

    /**
     * Event naming convention: {domain}.{aggregate}.{action}
     *
     * <p>Examples:
     * <ul>
     *   <li>invoice.created</li>
     *   <li>invoice.updated</li>
     *   <li>invoice.cancelled</li>
     *   <li>payment.processed</li>
     * </ul>
     */
    public static final String NAMING_CONVENTION = "%s.%s.%s";

    /**
     * Creates an event name following the platform convention.
     *
     * @param domain the domain name (e.g., "invoice")
     * @param aggregate the aggregate name (e.g., "invoice")
     * @param action the action (e.g., "created")
     * @return the event name
     */
    public static String createName(String domain, String aggregate, String action) {
        return NAMING_CONVENTION.formatted(domain, aggregate, action);
    }

    /**
     * Creates a versioned event name.
     *
     * @param eventName the base event name
     * @param version the version
     * @return the versioned event name
     */
    public static String versionedName(String eventName, String version) {
        return "%s.v%s".formatted(eventName, version);
    }

    /**
     * Extracts the domain from an event name.
     *
     * @param eventName the event name
     * @return the domain
     */
    public static String extractDomain(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return null;
        }
        String[] parts = eventName.split("\\.");
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Extracts the action from an event name.
     *
     * @param eventName the event name
     * @return the action
     */
    public static String extractAction(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return null;
        }
        String[] parts = eventName.split("\\.");
        return parts.length > 2 ? parts[2] : null;
    }
}
