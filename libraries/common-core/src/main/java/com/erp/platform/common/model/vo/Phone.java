package com.erp.platform.common.model.vo;

import java.util.Objects;

/**
 * Value object representing a phone number.
 *
 * <p>Ensures type safety and validates phone number format.
 * Supports international format with country code.
 *
 * @since 1.0.0
 */
public record Phone(String value) {

    private static final String PHONE_PATTERN = "^[+]?[0-9]{10,15}$";

    /**
     * Creates a new Phone instance.
     *
     * @param value the phone number
     * @throws IllegalArgumentException if phone is null, blank, or invalid
     */
    public Phone {
        Objects.requireNonNull(value, "Phone cannot be null");
        String cleaned = value.trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be blank");
        }
        // Remove common formatting characters
        String digits = cleaned.replaceAll("[\\s\\-()]", "");
        if (!digits.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Invalid phone format: " + value);
        }
    }

    /**
     * Creates a Phone instance, returning null if invalid.
     *
     * @param value the phone string
     * @return Phone instance or null if invalid
     */
    public static Phone ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new Phone(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns the phone number in E.164 format.
     *
     * @return the formatted phone number
     */
    public String toE164() {
        return value.replaceAll("[\\s\\-()]", "");
    }

    @Override
    public String toString() {
        return value;
    }
}
