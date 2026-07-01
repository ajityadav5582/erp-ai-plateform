package com.erp.platform.common.model.vo;

import java.util.Objects;

/**
 * Value object representing an email address.
 *
 * <p>Ensures type safety and validates email format on creation.
 * Immutable and thread-safe.
 *
 * @since 1.0.0
 */
public record Email(String value) {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    /**
     * Creates a new Email instance.
     *
     * @param value the email address
     * @throws IllegalArgumentException if email is null, blank, or invalid
     */
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!trimmed.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    /**
     * Creates an Email instance, returning null if invalid.
     *
     * @param value the email string
     * @return Email instance or null if invalid
     */
    public static Email ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new Email(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the domain part of the email.
     *
     * @return the email domain
     */
    public String getDomain() {
        int atIndex = value.lastIndexOf('@');
        return atIndex > 0 ? value.substring(atIndex + 1) : "";
    }

    /**
     * Returns the local part of the email.
     *
     * @return the email local part
     */
    public String getLocalPart() {
        int atIndex = value.lastIndexOf('@');
        return atIndex > 0 ? value.substring(0, atIndex) : value;
    }

    @Override
    public String toString() {
        return value;
    }
}
