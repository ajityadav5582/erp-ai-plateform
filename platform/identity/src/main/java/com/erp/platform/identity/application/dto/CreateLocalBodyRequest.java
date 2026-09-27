package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new local body.
 *
 * @since 1.0.0
 */
public record CreateLocalBodyRequest(
    @NotNull(message = "Province ID is required")
    Long provinceId,

    @NotBlank(message = "LocalBody code is required")
    @Size(min = 1, max = 20, message = "LocalBody code must be between 1 and 20 characters")
    String localBodyCode,

    @NotBlank(message = "LocalBody name is required")
    @Size(min = 1, max = 100, message = "LocalBody name must be between 1 and 100 characters")
    String localBodyName,

    @Size(max = 50, message = "LocalBody type must not exceed 50 characters")
    String localBodyType
) {
}
