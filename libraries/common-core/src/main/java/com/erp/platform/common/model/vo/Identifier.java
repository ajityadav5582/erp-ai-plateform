package com.erp.platform.common.model.vo;

import java.util.UUID;

/**
 * Value object representing a unique identifier.
 *
 * <p>Provides type-safe wrapper for UUID-based identifiers.
 * Ensures all identifiers are valid UUIDs.
 *
 * @since 1.0.0
 */
public record Identifier(UUID value) {

    /**
     * Creates a new Identifier with a generated UUID.
     *
     * @return a new Identifier
     */
    public static Identifier generate() {
        return new Identifier(UUID.randomUUID());
    }

    /**
     * Creates an Identifier from a UUID string.
     *
     * @param value the UUID string
     * @return a new Identifier
     * @throws IllegalArgumentException if the string is not a valid UUID
     */
    public static Identifier from(String value) {
        return new Identifier(UUID.fromString(value));
    }

    /**
     * Creates an Identifier from a UUID.
     *
     * @param value the UUID
     * @return a new Identifier
     */
    public static Identifier from(UUID value) {
        return new Identifier(value);
    }

    /**
     * Returns the identifier as a string.
     *
     * @return the UUID string
     */
    public String asString() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
