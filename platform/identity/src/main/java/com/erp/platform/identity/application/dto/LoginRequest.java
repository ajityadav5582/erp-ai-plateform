package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to authenticate a user and obtain tokens.
 *
 * @param tenantId   the tenant the user belongs to (multi-tenant isolation)
 * @param username   the user's username or email
 * @param password   the user's raw password (never logged)
 * @param deviceInfo optional best-effort device description (stored on the refresh token)
 * @param ipAddress  optional client IP address (stored on the refresh token)
 *
 * @since 1.0.0
 */
public record LoginRequest(
        @NotNull(message = "Tenant ID is required")
        Long tenantId,

        @NotBlank(message = "Username is required")
        @Size(max = 255, message = "Username must not exceed 255 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(max = 1024, message = "Password must not exceed 1024 characters")
        String password,

        @Size(max = 255, message = "Device info must not exceed 255 characters")
        String deviceInfo,

        @Size(max = 45, message = "IP address must not exceed 45 characters")
        String ipAddress) {
}
