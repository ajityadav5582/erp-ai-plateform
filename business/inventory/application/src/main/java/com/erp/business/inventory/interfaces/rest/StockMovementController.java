package com.erp.business.inventory.interfaces.rest;

import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.inventory.stock.InsufficientStockException;
import com.erp.business.inventory.stock.StockMovementBatchResult;
import com.erp.business.inventory.stock.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Inbound HTTP adapter for posting stock movements.
 *
 * <p>This is the endpoint Procurement calls to land a goods receipt in the stock
 * ledger. It is <strong>idempotent by reference</strong>: posting the same
 * {@code (referenceType, referenceId)} twice returns the movements already
 * written rather than duplicating them, so a caller can safely retry after a
 * timeout. Tenant and company come from the validated request context, never
 * from the body.
 */
@RestController
@RequestMapping("/api/v1/inventory/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;
    private final BusinessContextAccessor context;

    /** Goods arriving: an inbound (IN) batch for one source document. */
    @PostMapping("/receipts")
    public ResponseEntity<StockMovementBatchResponse> postReceipt(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.postReceiptBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /** Goods leaving: an outbound (OUT) batch that reverses a prior receipt. */
    @PostMapping("/reversals")
    public ResponseEntity<StockMovementBatchResponse> reverseReceipt(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.reverseReceiptBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /**
     * Goods leaving for a customer: an outbound (OUT) batch for a sales delivery.
     * Idempotent by reference, so a caller can safely retry after a timeout.
     */
    @PostMapping("/issues")
    public ResponseEntity<StockMovementBatchResponse> postIssue(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.postIssueBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /** Goods returning: an outbound (OUT) batch that reverses a prior issue. */
    @PostMapping("/issue-reversals")
    public ResponseEntity<StockMovementBatchResponse> reverseIssue(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.reverseIssueBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /**
     * An outbound issue that would take a warehouse's on-hand below zero is
     * refused here as a 422, so the caller (POS, sales delivery) surfaces a clear
     * "insufficient stock" message instead of a generic 500. The posting
     * transaction has already rolled back by the time this runs.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * A signed stock adjustment (count variance, damage, write-off). Line
     * quantities are signed: positive adds, negative removes. Idempotent by
     * reference; a negative line that would take on-hand below zero is refused as
     * a 422 unless the warehouse allows negative stock.
     */
    @PostMapping("/adjustments")
    public ResponseEntity<StockMovementBatchResponse> postAdjustment(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.postAdjustmentBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /** Soft-allocates stock against a demand (e.g. a confirmed sales order). */
    @PostMapping("/reservations")
    public ResponseEntity<StockMovementBatchResponse> reserve(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.reserveBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    /** Frees a prior reservation, returning quantity to available. */
    @PostMapping("/releases")
    public ResponseEntity<StockMovementBatchResponse> release(
            @Valid @RequestBody StockReceiptRequest request) {

        StockMovementBatchResult result = stockMovementService.releaseBatch(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(toResponse(request, result));
    }

    private StockMovementBatchResponse toResponse(StockReceiptRequest request, StockMovementBatchResult result) {
        List<Long> ids = result.movements().stream().map(StockMovement::getId).toList();
        return new StockMovementBatchResponse(
                request.referenceType(),
                request.referenceId(),
                result.postedCount(),
                result.alreadyExisted(),
                ids);
    }
}
