package com.erp.platform.common.model.vo;

import java.util.Objects;

/**
 * Value object representing a person's name.
 *
 * <p>Immutable and thread-safe name representation.
 *
 * @since 1.0.0
 */
public record Name(String firstName, String lastName) {

    /**
     * Creates a new Name instance.
     *
     * @throws IllegalArgumentException if any field is null or blank
     */
    public Name {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");

        if (firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }
    }

    /**
     * Creates a Name from a full name string.
     *
     * @param fullName the full name (first and last)
     * @return a new Name instance
     */
    public static Name of(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be null or blank");
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        return new Name(firstName, lastName);
    }

    /**
     * Returns the full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns the initials.
     *
     * @return the initials
     */
    public String getInitials() {
        return (firstName.charAt(0) + "." + lastName.charAt(0) + ".").toUpperCase();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
