package com.erp.business.inventory.items.dto;

import java.util.List;

public record ItemImportResult(
        int totalRows,
        int importedCount,
        int failedCount,
        List<RowError> errors
) {
    public record RowError(
            int rowNumber,
            String sku,
            String itemName,
            String error
    ) {}
}

