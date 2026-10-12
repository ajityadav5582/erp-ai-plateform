package com.erp.business.inventory.warehouse.dto;

import com.erp.business.inventory.domain.WarehouseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request to create an additional warehouse.
 *
 * <p>Note there is no {@code isDefault} field. A caller creating a second warehouse is
 * adding to the network, not choosing the company's fallback; promotion is a separate,
 * deliberate {@code POST /{id}/default}. Keeping the two apart is what stops a routine
 * "add a second site" from silently repointing where every future goods receipt lands.
 *
 * @param code    short unique code within the company
 * @param name    human-readable name
 * @param type    the kind of storage this provides
 * @param branchId the administrative branch it sits under, or null for company-wide
 * @param address optional street address
 * @param phone   optional contact phone
 */
public record CreateWarehouseRequest(

        @NotBlank(message = "Warehouse code is required")
        @Size(max = 64, message = "Warehouse code must not exceed 64 characters")
        String code,

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
        String name,

        @NotNull(message = "Warehouse type is required")
        WarehouseType type,

        @Positive(message = "Branch id must be a positive identifier")
        Long branchId,

        @Size(max = 500, message = "Warehouse address must not exceed 500 characters")
        String address,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone
) {
}
