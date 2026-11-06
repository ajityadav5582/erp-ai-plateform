package com.erpai.integration.ird.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a 9-digit Nepalese PAN.
 */
@Embeddable
public final class SellerPan {

    protected SellerPan() {
        this.value = null;
    }

    private static final Pattern PAN_PATTERN = Pattern.compile("\\d{9}");

    private String value;

    private SellerPan(String value) {
        this.value = value;
    }

    public static SellerPan of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Seller PAN must not be null");
        }
        String normalized = raw.trim();
        if (!PAN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Seller PAN must be exactly 9 digits, got: " + raw);
        }
        return new SellerPan(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SellerPan that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
