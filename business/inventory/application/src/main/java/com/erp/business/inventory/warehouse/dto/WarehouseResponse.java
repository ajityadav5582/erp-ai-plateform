package com.erp.business.inventory.warehouse.dto;

import com.erp.business.inventory.domain.WarehouseType;

import java.time.LocalDateTime;

/**
 * A warehouse as returned to the client.
 *
 * @param isDefault true when this is the company's fallback warehouse, into which
 *                  goods with no explicit destination are received
 */
public record WarehouseResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String code,
        String name,
        WarehouseType type,
        Long branchId,
        String address,
        String phone,
        Boolean isActive,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
