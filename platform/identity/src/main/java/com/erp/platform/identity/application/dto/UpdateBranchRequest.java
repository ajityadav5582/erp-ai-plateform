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

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(max = 100, message = "State must not exceed 100 characters")
    String state,

    @Size(max = 100, message = "Country must not exceed 100 characters")
    String country,

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    String postalCode,

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    String timezone,

    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    String currency,

    Long managerId
) {
}
