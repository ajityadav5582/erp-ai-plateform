package com.erp.platform.common.tenancy;

/**
 * Value object representing a tenant identifier.
 *
 * <p>Encapsulates tenant ID validation and provides
 * type-safe tenant identification.
 *
 * @since 1.0.0
 */
public record TenantIdentifier(String value) {

    /**
     * Creates a new tenant identifier.
     *
     * @param value the tenant ID value
     * @throws IllegalArgumentException if the value is null or blank
     */
    public TenantIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
    }

    /**
     * Gets the tenant ID value.
     *
     * @return the tenant ID
     */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
