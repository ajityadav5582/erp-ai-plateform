package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing province.
 *
 * <p>All fields are optional; only non-null fields are applied during update.
 *
 * @since 1.0.0
 */
public record UpdateProvinceRequest(
    @Size(min = 1, max = 20, message = "Province code must be between 1 and 20 characters")
    String provinceCode,

    @Size(min = 1, max = 100, message = "Province name must be between 1 and 100 characters")
    String provinceName,

    @Size(max = 100, message = "Nepali name must not exceed 100 characters")
    String nepaliName,

    @Size(min = 3, max = 3, message = "Country code must be a 3-character ISO code")
    String countryCode
) {
}
