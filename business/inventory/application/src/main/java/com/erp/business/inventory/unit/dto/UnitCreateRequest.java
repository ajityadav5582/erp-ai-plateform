package com.erp.business.inventory.unit.dto;

import com.erp.business.inventory.domain.UnitDimension;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a unit.
 *
 * <p>A record is correct here because every field is required to be present on a
 * create: there is no absent-versus-null distinction to preserve, and the
 * compact canonical constructor gives us the validation for free.
 *
 * <p>The constraints deliberately duplicate the checks in {@code Unit.create()}.
 * The annotations produce a 400 with a per-field message before the service is
 * entered; the domain guards remain the authoritative check for any caller that
 * bypasses the web layer.
 */
public record UnitCreateRequest(

        @NotBlank(message = "Unit name is required")
        @Size(max = 100, message = "Unit name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Unit code is required")
        @Size(max = 32, message = "Unit code must not exceed 32 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_/\\-]*$",
                message = "Unit code may contain only letters, digits, spaces, and the characters _ - /"
        )
        String code,

        @NotNull(message = "Unit dimension is required")
        UnitDimension dimension,

        @Size(max = 16, message = "Unit symbol must not exceed 16 characters")
        String symbol,

        @Min(value = 0, message = "Decimal scale cannot be negative")
        @Max(value = 6, message = "Decimal scale must not exceed 6 digits")
        Integer decimalScale
) {
}
