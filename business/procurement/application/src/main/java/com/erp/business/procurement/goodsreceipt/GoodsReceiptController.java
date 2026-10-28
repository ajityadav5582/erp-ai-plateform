package com.erp.business.procurement.goodsreceipt;

import com.erp.business.procurement.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.erp.business.procurement.goodsreceipt.dto.GoodsReceiptResponse;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderDecisionRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.platform.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Goods receipt endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body runs. The
 * controller only reads the authenticated tenant and company, so a caller cannot widen its
 * own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/procurement/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

    private static final String BASE_PATH = "/api/v1/procurement/goods-receipts";

    private final GoodsReceiptService receiptService;
    private final BusinessContextAccessor context;

    /**
     * Records a delivery against a purchase order.
     *
     * <p>POST rather than PUT because receiving is an event, not a replacement of the
     * order: repeating it against the same order is refused by the order's own received
     * quantities, so the operation is not idempotent and must not be modelled as PUT.
     */
    @PostMapping
    public ResponseEntity<GoodsReceiptResponse> record(
            @Valid @RequestBody CreateGoodsReceiptRequest request,
            UriComponentsBuilder uriBuilder) {

        GoodsReceiptResponse response = receiptService.record(
                context.getTenantId(), context.getCompanyId(), request, currentUser());

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Voids a goods receipt and returns its quantities to the order.
     *
     * <p>POST rather than DELETE: the note is not removed, it is voided and kept
     * as part of the audit trail, so this is a state change on an existing
     * resource rather than the removal of one.
     */
    @PostMapping("/{goodsReceiptId}/cancel")
    public ResponseEntity<GoodsReceiptResponse> cancel(
            @PathVariable Long goodsReceiptId,
            @Valid @RequestBody PurchaseOrderDecisionRequest request) {

        return ResponseEntity.ok(receiptService.cancel(
                context.getTenantId(), context.getCompanyId(), goodsReceiptId,
                request.reason(), currentUser()));
    }

    /**
     * Re-attempts landing a receipt's stock movement in Inventory.
     *
     * <p>For a receipt recorded while Inventory was unreachable, whose stock
     * post is therefore FAILED. The call is idempotent, so it is also safe on a
     * receipt that in fact posted - it simply returns it unchanged.
     */
    @PostMapping("/{goodsReceiptId}/post-stock")
    public ResponseEntity<GoodsReceiptResponse> retryStockPost(
            @PathVariable Long goodsReceiptId) {

        return ResponseEntity.ok(receiptService.retryStockPost(
                context.getTenantId(), context.getCompanyId(), goodsReceiptId));
    }

    @GetMapping("/{goodsReceiptId}")
    public ResponseEntity<GoodsReceiptResponse> getById(@PathVariable Long goodsReceiptId) {
        return ResponseEntity.ok(receiptService.getById(
                context.getTenantId(), context.getCompanyId(), goodsReceiptId));
    }

    @GetMapping
    public ResponseEntity<Page<GoodsReceiptResponse>> findAll(
            @PageableDefault(size = 20, sort = "receiptDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(receiptService.findAll(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    /** The receipts raised against one purchase order. */
    @GetMapping("/by-order/{purchaseOrderId}")
    public ResponseEntity<Page<GoodsReceiptResponse>> findByOrder(
            @PathVariable Long purchaseOrderId,
            @PageableDefault(size = 20, sort = "receiptDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(receiptService.findByOrder(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId, pageable));
    }

    private String currentUser() {
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("An authenticated user is required for this operation");
        }
        return userId;
    }
}
