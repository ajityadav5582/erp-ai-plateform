package com.erp.platform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new branch.
 *
 * <p>A branch carries a free-text address and an optional ward number.
 *
 * @since 1.0.0
 */
public record CreateBranchRequest(
    @NotBlank(message = "Branch code is required")
    @Size(min = 1, max = 50, message = "Branch code must be between 1 and 50 characters")
    String branchCode,

    @NotBlank(message = "Branch name is required")
    @Size(min = 1, max = 200, message = "Branch name must be between 1 and 200 characters")
    String branchName,

    @Size(max = 500, message = "Address must not exceed 500 characters")
    String address,

    @Size(max = 50, message = "Ward number must not exceed 50 characters")
    String wardNo
) {
}
