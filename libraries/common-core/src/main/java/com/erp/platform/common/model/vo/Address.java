package com.erp.platform.common.model.vo;

import java.util.Objects;

/**
 * Value object representing a physical address.
 *
 * <p>Immutable and thread-safe address representation.
 *
 * @since 1.0.0
 */
public record Address(
    String street,
    String city,
    String state,
    String country,
    String postalCode
) {

    /**
     * Creates a new Address instance.
     *
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        if (street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("City cannot be blank");
        }
        if (country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be blank");
        }
    }

    /**
     * Creates an Address with only required fields.
     *
     * @param street the street address
     * @param city the city
     * @param country the country
     * @return a new Address
     */
    public static Address of(String street, String city, String country) {
        return new Address(street, city, null, country, null);
    }

    /**
     * Returns the full address as a single string.
     *
     * @return formatted address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        if (city != null && !city.isBlank()) {
            sb.append(", ").append(city);
        }
        if (state != null && !state.isBlank()) {
            sb.append(", ").append(state);
        }
        if (postalCode != null && !postalCode.isBlank()) {
            sb.append(" ").append(postalCode);
        }
        sb.append(", ").append(country);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFullAddress();
    }
}
