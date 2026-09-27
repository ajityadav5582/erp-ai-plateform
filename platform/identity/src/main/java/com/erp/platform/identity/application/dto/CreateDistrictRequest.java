package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new district.
 *
 * @since 1.0.0
 */
public record CreateDistrictRequest(
    @NotNull(message = "Province ID is required")
    Long provinceId,

    @NotBlank(message = "District code is required")
    @Size(min = 1, max = 20, message = "District code must be between 1 and 20 characters")
    String districtCode,

    @NotBlank(message = "District name is required")
    @Size(min = 1, max = 100, message = "District name must be between 1 and 100 characters")
    String districtName,

    @Size(max = 100, message = "Nepali name must not exceed 100 characters")
    String nepaliName,

    @Size(min = 3, max = 3, message = "Country code must be a 3-character ISO code")
    String countryCode
) {
}
