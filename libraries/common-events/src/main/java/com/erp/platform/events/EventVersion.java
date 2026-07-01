package com.erp.platform.events;

/**
 * Represents an event version for schema evolution.
 *
 * <p>Event versions follow semantic versioning principles
 * to support backward and forward compatibility.
 *
 * @since 1.0.0
 */
public record EventVersion(int major, int minor, int patch) implements Comparable<EventVersion> {

    /**
     * Default event version (1.0.0).
     */
    public static final EventVersion DEFAULT = new EventVersion(1, 0, 0);

    /**
     * Creates a new event version from a string.
     *
     * @param version the version string (e.g., "1.0.0")
     * @return the parsed event version
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static EventVersion from(String version) {
        if (version == null || version.isBlank()) {
            return DEFAULT;
        }

        String[] parts = version.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid event version format: " + version);
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return new EventVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid event version format: " + version, e);
        }
    }

    @Override
    public int compareTo(EventVersion other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.minor, other.minor);
        if (result != 0) {
            return result;
        }
        return Integer.compare(this.patch, other.patch);
    }

    /**
     * Checks if this version is compatible with another version.
     *
     * <p>Versions are compatible if they have the same major version.
     *
     * @param other the other version
     * @return true if compatible
     */
    public boolean isCompatibleWith(EventVersion other) {
        return this.major == other.major;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
