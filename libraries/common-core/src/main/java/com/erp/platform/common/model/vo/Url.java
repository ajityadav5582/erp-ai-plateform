package com.erp.platform.common.model.vo;

import java.util.Objects;

/**
 * Value object representing a URL.
 *
 * <p>Ensures type safety and validates URL format on creation.
 * Immutable and thread-safe.
 *
 * @since 1.0.0
 */
public record Url(String value) {

    /**
     * Creates a new Url instance.
     *
     * @param value the URL string
     * @throws IllegalArgumentException if URL is null, blank, or invalid
     */
    public Url {
        Objects.requireNonNull(value, "URL cannot be null");
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("URL cannot be blank");
        }
        if (!trimmed.matches("^https?://.*")) {
            throw new IllegalArgumentException("Invalid URL format: " + value);
        }
    }

    /**
     * Creates a Url instance, returning null if invalid.
     *
     * @param value the URL string
     * @return Url instance or null if invalid
     */
    public static Url ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new Url(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the protocol of the URL.
     *
     * @return the protocol (http, https, etc.)
     */
    public String getProtocol() {
        int protocolEnd = value.indexOf("://");
        return protocolEnd > 0 ? value.substring(0, protocolEnd) : "";
    }

    /**
     * Returns the host of the URL.
     *
     * @return the host
     */
    public String getHost() {
        try {
            java.net.URI uri = new java.net.URI(value);
            return uri.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
