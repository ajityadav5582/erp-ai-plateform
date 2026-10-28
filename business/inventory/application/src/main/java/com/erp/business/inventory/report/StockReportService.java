package com.erp.business.inventory.report;

import com.erp.business.inventory.domain.report.LowStockView;
import com.erp.business.inventory.domain.report.StockAgingView;
import com.erp.business.inventory.domain.report.NegativeStockView;
import com.erp.business.inventory.domain.report.StockValuationView;
import com.erp.business.inventory.domain.report.StockOnHandView;
import com.erp.business.inventory.report.dto.StockCardResponse;
import com.erp.business.inventory.report.dto.StockMovementSummaryResponse;
import com.erp.business.inventory.report.dto.StockValuationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Read-only stock reporting queries.
 *
 * <p>Inventory endpoints are authenticated and tenant/company-scoped from the
 * request context (this service does not wire the fine-grained permission
 * aspect), matching the rest of the Inventory API.
 */
public interface StockReportService {

    /**
     * Current stock levels, one row per (warehouse, item, unit), with display
     * names resolved.
     *
     * @param warehouseId optional warehouse filter
     * @param itemId      optional item filter
     * @param inStockOnly when true, only rows with on-hand > 0
     */
    Page<StockOnHandView> onHand(Long tenantId, Long companyId,
                                 Long warehouseId, Long itemId, boolean inStockOnly,
                                 Pageable pageable);

    /**
     * Stock card for one item over a period: opening balance, each movement with
     * its on-hand effect and the running balance, and the closing balance.
     */
    StockCardResponse stockCard(Long tenantId, Long companyId, Long itemId,
                                Long warehouseId, String type, Instant from, Instant to);

    /** Period movement summary: per item/warehouse/unit opening + in − out ± adjust = closing. */
    StockMovementSummaryResponse summarize(Long tenantId, Long companyId, Long warehouseId,
                                           Instant from, Instant to);

    /**
     * Low-stock / reorder report: rows whose available quantity is at or below the
     * item's reorder threshold, most urgent first, with the suggested order
     * quantity. Items that have never been stocked are included.
     *
     * @param warehouseId optional warehouse filter
     */
    Page<LowStockView> lowStock(Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    /**
     * Stock valuation: rows carrying a non-zero quantity valued at the item's
     * purchase price, most valuable first, with grand totals over every matching
     * row.
     *
     * <p>Indicative only — the cost is the item master's purchase price, not the
     * cost actually paid on the movement that brought the stock in.
     *
     * @param warehouseId optional warehouse filter
     */
    StockValuationResponse valuation(Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    /**
     * Negative-stock report: balances whose on-hand quantity has gone below zero,
     * most negative first. A data-quality check, not a stock level.
     *
     * @param warehouseId optional warehouse filter
     */
    Page<NegativeStockView> negativeStock(Long tenantId, Long companyId, Long warehouseId,
                                          Pageable pageable);

    /**
     * Stock aging report: balances holding stock with how long they have sat
     * since the last movement, oldest first, bucketed from fresh to dead.
     *
     * @param warehouseId optional warehouse filter
     * @param minDays     only rows idle at least this many days; zero for all
     */
    Page<StockAgingView> aging(Long tenantId, Long companyId, Long warehouseId, long minDays,
                               Pageable pageable);
}
