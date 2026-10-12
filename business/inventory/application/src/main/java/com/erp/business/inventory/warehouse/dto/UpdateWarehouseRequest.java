package com.erp.business.inventory.warehouse.dto;

import com.erp.business.inventory.domain.WarehouseType;
import jakarta.validation.constraints.Size;

/**
 * Partial update to a warehouse. Every field is optional; {@code null} or blank leaves
 * the stored value untouched.
 *
 * <p>{@code branchId} is absent on purpose. A partial update cannot distinguish
 * "leave the branch alone" from "clear the branch", and guessing wrong would silently
 * detach a warehouse from the branch whose approval chain governs it. Detaching, if it
 * is ever needed, should be its own explicit endpoint.
 *
 * @param code    new short code, or null to keep the current one
 * @param name    new name, or null to keep the current one
 * @param type    new type, or null to keep the current one
 * @param address new address, or null to keep the current one
 * @param phone   new phone, or null to keep the current one
 */
public record UpdateWarehouseRequest(

        @Size(max = 64, message = "Warehouse code must not exceed 64 characters")
        String code,

        @Size(max = 255, message = "Warehouse name must not exceed 255 characters")
        String name,

        WarehouseType type,

        @Size(max = 500, message = "Warehouse address must not exceed 500 characters")
        String address,

        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone
) {
}
