package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user self-registration.
 *
 * @param tenantId         the tenant ID (must be explicitly provided)
 * @param username         the unique username within the tenant
 * @param email            the email address
 * @param password         the raw user password
 * @param firstName        the user's first name
 * @param lastName         the user's last name
 * @param phoneNumber      optional phone number
 * @param jobTitle         optional job title
 * @param profileImageUrl  optional profile image URL
 * @param branchId         optional branch assignment
 * @param departmentId     optional department assignment
 * @param deviceInfo       optional device info
 * @param ipAddress        optional IP address
 *
 * @since 1.0.0
 */
public record RegisterRequest(
        @NotNull(message = "Tenant ID is required")
        Long tenantId,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber,

        @Size(max = 100, message = "Job title must not exceed 100 characters")
        String jobTitle,

        @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
        String profileImageUrl,

        Long branchId,

        Long departmentId,

        @Size(max = 255, message = "Device info must not exceed 255 characters")
        String deviceInfo,

        @Size(max = 45, message = "IP address must not exceed 45 characters")
        String ipAddress
) {
}
