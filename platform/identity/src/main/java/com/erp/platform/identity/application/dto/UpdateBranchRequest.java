package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing branch.
 *
 * <p>All fields are optional; only non-null fields are applied during update.
 *
 * @since 1.0.0
 */
public record UpdateBranchRequest(
    @Size(min = 1, max = 50, message = "Branch code must be between 1 and 50 characters")
    String branchCode,

    @Size(min = 1, max = 200, message = "Branch name must be between 1 and 200 characters")
    String branchName,

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    String phone,

    @Size(max = 500, message = "Address must not exceed 500 characters")
    String address,

    @Size(max = 50, message = "Local level ID must not exceed 50 characters")
    String localLevelId,

    @Size(max = 50, message = "Ward number must not exceed 50 characters")
    String wardNo
) {
}
