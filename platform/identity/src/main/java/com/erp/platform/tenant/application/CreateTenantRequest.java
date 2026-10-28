package com.erp.platform.tenant.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a tenant.
 *
 * @since 1.0.0
 */
public record CreateTenantRequest(
        @NotBlank(message = "Tenant code is required")
        String tenantCode,

        @NotBlank(message = "Legal name is required")
        String legalName,

        @NotBlank(message = "Display name is required")
        String displayName,

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Website is required")
        String website,

        @NotBlank(message = "Timezone is required")
        String timezone,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Language is required")
        String language,

        @NotNull(message = "Isolation strategy is required")
        String isolationStrategy
) {
}
