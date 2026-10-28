package com.erp.business.pos.register.dto;

import com.erp.business.pos.domain.PosRegisterStatus;

import java.time.LocalDateTime;

/**
 * Read model for a POS register.
 */
public record PosRegisterResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String registerCode,
        String registerName,
        Long branchId,
        String branchName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        PosRegisterStatus status,
        Long currentSessionId,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version
) {
}
