package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to initiate a password reset for a user identified by email within a tenant.
 *
 * @param tenantId the tenant the user belongs to (multi-tenant isolation)
 * @param email    the user's email address
 *
 * @since 1.0.0
 */
public record PasswordResetRequest(
        @NotNull(message = "Tenant ID is required")
        Long tenantId,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email) {
}
