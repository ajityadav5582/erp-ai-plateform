package com.erp.platform.identity.application.dto;

/**
 * Response returned by the forgot-password endpoint.
 *
 * <p>For security, the message is always generic. The {@code resetToken} field is only
 * populated when {@code auth.password-reset.return-token-in-response} is enabled (development
 * environments); in production the token is delivered out-of-band (e.g. email) and this
 * field is {@code null}.
 *
 * @param message     a generic, non-disclosing message
 * @param resetToken  the raw reset token, or {@code null} when not returned in the response
 *
 * @since 1.0.0
 */
public record PasswordResetResponse(
        String message,
        String resetToken) {

    public static PasswordResetResponse of(String message) {
        return new PasswordResetResponse(message, null);
    }

    public static PasswordResetResponse of(String message, String resetToken) {
        return new PasswordResetResponse(message, resetToken);
    }
}
