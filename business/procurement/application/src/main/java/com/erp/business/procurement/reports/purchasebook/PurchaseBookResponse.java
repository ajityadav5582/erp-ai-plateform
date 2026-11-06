package com.erp.business.procurement.reports.purchasebook;

import java.util.List;

public record PurchaseBookResponse(
        List<PurchaseBookRow> rows,
        PurchaseBookTotals totals,
        int page,
        int size,
        long totalElements
) {
}
