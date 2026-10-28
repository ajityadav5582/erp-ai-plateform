package com.erp.business.inventory.report;

import com.erp.business.inventory.domain.report.LowStockView;
import com.erp.business.inventory.domain.report.StockAgingView;
import com.erp.business.inventory.domain.report.NegativeStockView;
import com.erp.business.inventory.domain.report.StockOnHandView;
import com.erp.business.inventory.report.dto.StockCardResponse;
import com.erp.business.inventory.report.dto.StockMovementSummaryResponse;
import com.erp.business.inventory.report.dto.StockValuationResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Read-only stock reports: on-hand levels, the stock card (ledger), the period
 * movement summary and the low-stock / reorder list.
 *
 * <p>Tenant and company come from the validated request context, never the
 * query string, so a caller cannot read another company's stock.
 */
@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
public class StockReportController {

    private final StockReportService stockReportService;
    private final BusinessContextAccessor context;

    /** Current stock levels per (warehouse, item, unit). */
    @GetMapping("/on-hand")
    public ResponseEntity<Page<StockOnHandView>> onHand(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            Pageable pageable) {
        return ResponseEntity.ok(stockReportService.onHand(
                context.getTenantId(), context.getCompanyId(), warehouseId, itemId, inStockOnly, pageable));
    }

    /** Stock card (ledger) for one item over a period, with running balance. */
    @GetMapping("/ledger")
    public ResponseEntity<StockCardResponse> ledger(
            @RequestParam Long itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(stockReportService.stockCard(
                context.getTenantId(), context.getCompanyId(), itemId, warehouseId, type, from, to));
    }

    /** Period stock movement summary (aggregated per item/warehouse/unit). */
    @GetMapping("/summary")
    public ResponseEntity<StockMovementSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(stockReportService.summarize(
                context.getTenantId(), context.getCompanyId(), warehouseId, from, to));
    }

    /**
     * Low-stock / reorder report: rows at or below their reorder threshold, most
     * urgent first, with the suggested order quantity.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<Page<LowStockView>> lowStock(
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable) {
        return ResponseEntity.ok(stockReportService.lowStock(
                context.getTenantId(), context.getCompanyId(), warehouseId, pageable));
    }

    /**
     * Stock valuation: rows valued at the item's purchase price, most valuable
     * first, with grand totals over every matching row.
     */
    @GetMapping("/valuation")
    public ResponseEntity<StockValuationResponse> valuation(
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable) {
        return ResponseEntity.ok(stockReportService.valuation(
                context.getTenantId(), context.getCompanyId(), warehouseId, pageable));
    }

    /**
     * Negative-stock report: balances below zero, most negative first. A
     * data-quality check rather than a stock level.
     */
    @GetMapping("/negative")
    public ResponseEntity<Page<NegativeStockView>> negativeStock(
            @RequestParam(required = false) Long warehouseId,
            Pageable pageable) {
        return ResponseEntity.ok(stockReportService.negativeStock(
                context.getTenantId(), context.getCompanyId(), warehouseId, pageable));
    }

    /**
     * Stock aging report: balances holding stock with how long they have sat
     * since the last movement, oldest first.
     *
     * @param minDays only rows idle at least this many days; zero for all
     */
    @GetMapping("/aging")
    public ResponseEntity<Page<StockAgingView>> aging(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "0") long minDays,
            Pageable pageable) {
        return ResponseEntity.ok(stockReportService.aging(
                context.getTenantId(), context.getCompanyId(), warehouseId, minDays, pageable));
    }
}
