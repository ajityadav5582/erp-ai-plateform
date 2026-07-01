package com.erp.platform.events.versioning;

/**
 * Event versioning support.
 *
 * <p>Provides utilities for managing event schema versions
 * and ensuring backward compatibility.
 *
 * @since 1.0.0
 */
public final class EventVersion {

    private EventVersion() {
        // Utility class
    }

    /**
     * Current event schema version.
     */
    public static final String CURRENT_VERSION = "1.0.0";

    /**
     * Version pattern for event names.
     */
    public static final String VERSION_PATTERN = "%s.v%s";

    /**
     * Returns the versioned event name.
     *
     * @param eventName the base event name
     * @param version the version
     * @return the versioned event name
     */
    public static String versionedName(String eventName, String version) {
        return VERSION_PATTERN.formatted(eventName, version);
    }

    /**
     * Returns the current versioned event name.
     *
     * @param eventName the base event name
     * @return the versioned event name
     */
    public static String currentVersionedName(String eventName) {
        return versionedName(eventName, CURRENT_VERSION);
    }

    /**
     * Checks if a version is compatible with the current version.
     *
     * @param eventVersion the event version
     * @return true if compatible
     */
    public static boolean isCompatible(String eventVersion) {
        if (eventVersion == null || eventVersion.isBlank()) {
            return false;
        }
        return eventVersion.equals(CURRENT_VERSION) || eventVersion.startsWith("1.");
    }
}
