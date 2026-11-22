package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.dto.PurchaseInvoiceResponse;
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
 * Supplier invoice endpoints.
 *
 * <p>Authorisation lives on the service: every method carries {@code @RequiresPermission},
 * enforced by the shared aspect before the body runs. The controller only reads the
 * authenticated tenant and company.
 */
@RestController
@RequestMapping("/api/v1/procurement/purchase-invoices")
@RequiredArgsConstructor
public class PurchaseInvoiceController {

    private static final String BASE_PATH = "/api/v1/procurement/purchase-invoices";

    private final PurchaseInvoiceService invoiceService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PurchaseInvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            UriComponentsBuilder uriBuilder) {

        PurchaseInvoiceResponse response = invoiceService.create(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    /**
     * Fast-match flow: atomically records the Goods Receipt and matching Purchase Invoice
     * against an approved Purchase Order when delivery and tax invoice arrive simultaneously.
     */
    @PostMapping("/fast-match-po")
    public ResponseEntity<PurchaseInvoiceResponse> fastMatchPo(
            @Valid @RequestBody com.erp.business.procurement.purchaseinvoice.dto.FastMatchPoRequest request,
            UriComponentsBuilder uriBuilder) {

        PurchaseInvoiceResponse response = invoiceService.fastMatchPurchaseOrder(
                context.getTenantId(), context.getCompanyId(), request, currentUser());

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<PurchaseInvoiceResponse> getById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getById(
                context.getTenantId(), context.getCompanyId(), invoiceId));
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseInvoiceResponse>> findAll(
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(invoiceService.findAll(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    /** Invoices still owing money: the accounts payable worklist. Optionally filtered by supplier. */
    @GetMapping("/outstanding")
    public ResponseEntity<Page<PurchaseInvoiceResponse>> findOutstanding(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long supplierId,
            @PageableDefault(size = 20, sort = "dueDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(invoiceService.findOutstanding(
                context.getTenantId(), context.getCompanyId(), supplierId, pageable));
    }

    private String currentUser() {
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("An authenticated user is required for this operation");
        }
        return userId;
    }
}
