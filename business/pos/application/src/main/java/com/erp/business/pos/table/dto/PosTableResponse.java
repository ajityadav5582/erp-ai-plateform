package com.erp.business.pos.table.dto;

public record PosTableResponse(
        Long id,
        String tableName,
        Long branchId,
        String status
) {
}
