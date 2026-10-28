package com.erp.business.inventory.supplier.dto;

import com.erp.business.inventory.domain.SupplierType;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String name,
        String code,
        SupplierType type,
        String email,
        String phone,
        String address,
        String taxId,
        Integer paymentTermsDays,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
