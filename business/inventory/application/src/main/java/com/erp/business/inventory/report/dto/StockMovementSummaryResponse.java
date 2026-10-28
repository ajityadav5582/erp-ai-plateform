package com.erp.business.inventory.report.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Period stock movement summary: per item/warehouse/unit rows plus grand totals. */
public record StockMovementSummaryResponse(
        Instant from,
        Instant to,
        BigDecimal totalOpening,
        BigDecimal totalIn,
        BigDecimal totalOut,
        BigDecimal totalAdjust,
        BigDecimal totalClosing,
        List<StockMovementSummaryRow> rows
) {
}
