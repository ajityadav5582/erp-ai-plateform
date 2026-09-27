package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new province/state.
 *
 * @since 1.0.0
 */
public record CreateProvinceRequest(
    @NotBlank(message = "Province code is required")
    @Size(min = 1, max = 20, message = "Province code must be between 1 and 20 characters")
    String provinceCode,

    @NotBlank(message = "Province name is required")
    @Size(min = 1, max = 100, message = "Province name must be between 1 and 100 characters")
    String provinceName,

    @Size(min = 3, max = 3, message = "Country code must be a 3-character ISO code")
    String countryCode
) {
}
