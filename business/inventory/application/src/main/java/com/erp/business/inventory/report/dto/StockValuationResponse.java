package com.erp.business.inventory.report.dto;

import com.erp.business.inventory.domain.report.StockValuationView;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Stock valuation report: the paged per-item rows plus grand totals that cover
 * every matching row, not just the current page.
 *
 * @param itemsValued      rows carrying a purchase price, so contributing value
 * @param itemsMissingCost rows with stock but no purchase price, so valued at
 *                         zero — the total is understated by this many rows
 */
public record StockValuationResponse(
        BigDecimal totalOnHandValue,
        BigDecimal totalReservedValue,
        BigDecimal totalAvailableValue,
        long itemsValued,
        long itemsMissingCost,
        Page<StockValuationView> rows
) {
}
