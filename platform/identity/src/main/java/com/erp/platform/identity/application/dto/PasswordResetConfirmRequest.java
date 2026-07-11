package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to complete a password reset using a reset token delivered out-of-band.
 *
 * @param token       the raw password-reset token (returned by the forgot-password call in dev)
 * @param newPassword the new raw password (will be BCrypt-hashed server-side)
 *
 * @since 1.0.0
 */
public record PasswordResetConfirmRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 1024, message = "New password must be between 8 and 1024 characters")
        String newPassword) {
}
