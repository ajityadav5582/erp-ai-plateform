package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to refresh an access token using a previously issued refresh token.
 *
 * @param refreshToken the opaque refresh token presented by the client
 *
 * @since 1.0.0
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
