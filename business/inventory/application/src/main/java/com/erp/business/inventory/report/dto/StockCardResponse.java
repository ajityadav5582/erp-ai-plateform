package com.erp.business.inventory.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Stock card for one item over a period: opening balance, movements, closing. */
public record StockCardResponse(
        Long itemId,
        String sku,
        String itemName,
        Instant from,
        Instant to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<StockCardRow> rows
) {
}
