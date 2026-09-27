package com.erp.platform.identity.application.dto;


/**
 * Response DTO for successful account registration.
 *
 * @param userId the registered user's business identifier
 * @param username the registered user's username
 * @param email the registered user's email
 * @param fullName the registered user's full name
 * @param message registration status message
 *
 * @since 1.0.0
 */
public record OnboardingResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String message
) {
}
