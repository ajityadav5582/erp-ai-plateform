package com.erp.business.pos.register.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a POS register.
 *
 * <p>Tenant and company are absent by design: they are read from the authenticated
 * request context, never from the body. The warehouse is mandatory because every
 * completed transaction deducts stock from it.
 */
public record CreatePosRegisterRequest(

        @NotBlank(message = "Register code is required")
        @Size(max = 40, message = "Register code must not exceed 40 characters")
        String registerCode,

        @NotBlank(message = "Register name is required")
        @Size(max = 255, message = "Register name must not exceed 255 characters")
        String registerName,

        @NotNull(message = "Warehouse is required")
        Long warehouseId,

        @NotBlank(message = "Warehouse code is required")
        @Size(max = 64, message = "Warehouse code must not exceed 64 characters")
        String warehouseCode,

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
        String warehouseName,

        Long branchId,

        @Size(max = 255, message = "Branch name must not exceed 255 characters")
        String branchName,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
