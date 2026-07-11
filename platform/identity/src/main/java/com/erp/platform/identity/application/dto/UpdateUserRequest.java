package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing user.
 *
 * @since 1.0.0
 */
public record UpdateUserRequest(
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email,

    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName,

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName,

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phoneNumber,

    @Size(max = 100, message = "Job title must not exceed 100 characters")
    String jobTitle,

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    String profileImageUrl,

    Long branchId,

    Long departmentId
) {
}
