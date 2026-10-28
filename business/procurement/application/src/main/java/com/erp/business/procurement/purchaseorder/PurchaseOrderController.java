package com.erp.business.procurement.purchaseorder;

import com.erp.business.procurement.purchaseorder.dto.ConvertToPurchaseOrderRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderDecisionRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Purchase order endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body runs.
 * The controller only reads the authenticated tenant, company and user, so a caller
 * cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/procurement/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final String BASE_PATH = "/api/v1/procurement/purchase-orders";

    private final PurchaseOrderConversionService conversionService;
    private final PurchaseOrderLifecycleService lifecycleService;
    private final BusinessContextAccessor context;

    /**
     * Converts an approved requisition into a draft order.
     *
     * <p>POST rather than PUT because the requisition id names the resource being
     * consumed: repeating the call is refused by the source requisition's own state
     * and by a unique key, so the operation is not idempotent and must not be
     * modelled as a replacement.
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> convert(
            @Valid @RequestBody ConvertToPurchaseOrderRequest request,
            UriComponentsBuilder uriBuilder) {

        PurchaseOrderResponse response = conversionService.convert(
                context.getTenantId(), context.getCompanyId(), request, currentUser());

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * The order book.
     *
     * <p>Newest first by order date, which is the order a buyer reading the list expects:
     * the document they raised this morning belongs above the one from last quarter. The
     * id is the tie-breaker that Spring adds automatically, so two orders raised on the
     * same date do not swap places between pages.
     */
    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponse>> findAll(
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(conversionService.findAll(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    /** The orders still owing goods, which is what the receiving team works from. */
    @GetMapping("/outstanding")
    public ResponseEntity<Page<PurchaseOrderResponse>> findOutstanding(
            @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(conversionService.findOutstanding(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(conversionService.getById(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId));
    }

    @PostMapping("/{purchaseOrderId}/submit")
    public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(lifecycleService.submit(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId, currentUser()));
    }

    @PostMapping("/{purchaseOrderId}/approve")
    public ResponseEntity<PurchaseOrderResponse> approve(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(lifecycleService.approve(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId, currentUser()));
    }

    @PostMapping("/{purchaseOrderId}/reject")
    public ResponseEntity<PurchaseOrderResponse> reject(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderDecisionRequest request) {

        return ResponseEntity.ok(lifecycleService.reject(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId,
                request, currentUser()));
    }

    @PostMapping("/{purchaseOrderId}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancel(
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderDecisionRequest request) {

        return ResponseEntity.ok(lifecycleService.cancel(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId,
                request, currentUser()));
    }

    @PostMapping("/{purchaseOrderId}/close")
    public ResponseEntity<PurchaseOrderResponse> close(@PathVariable Long purchaseOrderId) {
        return ResponseEntity.ok(lifecycleService.close(
                context.getTenantId(), context.getCompanyId(), purchaseOrderId, currentUser()));
    }

    private String currentUser() {
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("An authenticated user is required for this operation");
        }
        return userId;
    }
}
