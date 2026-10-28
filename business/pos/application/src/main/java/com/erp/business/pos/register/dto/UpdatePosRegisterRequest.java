package com.erp.business.pos.register.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to update a register's editable details. The warehouse assignment is
 * not updatable here; it is fixed at creation.
 */
public record UpdatePosRegisterRequest(

        @NotBlank(message = "Register name is required")
        @Size(max = 255, message = "Register name must not exceed 255 characters")
        String registerName,

        Long branchId,

        @Size(max = 255, message = "Branch name must not exceed 255 characters")
        String branchName,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
