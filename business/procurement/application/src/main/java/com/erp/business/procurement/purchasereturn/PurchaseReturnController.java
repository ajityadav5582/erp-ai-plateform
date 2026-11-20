package com.erp.business.procurement.purchasereturn;

import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.dto.CreatePurchaseReturnRequest;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnResponse;
import com.erp.business.procurement.purchasereturn.dto.ReturnDecisionRequest;
import com.erp.business.procurement.purchasereturn.dto.ReturnableLineResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
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

import java.util.List;

/**
 * Purchase Return / Debit Note REST Controller.
 */
@RestController
@RequestMapping("/api/v1/procurement/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private static final String BASE_PATH = "/api/v1/procurement/purchase-returns";

    private final PurchaseReturnService returnService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<PurchaseReturnResponse> create(
            @Valid @RequestBody CreatePurchaseReturnRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        PurchaseReturnResponse response = returnService.createReturn(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseReturnResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.getReturnById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping("/number/{debitNoteNumber}")
    public ResponseEntity<PurchaseReturnResponse> getByDebitNoteNumber(@PathVariable String debitNoteNumber) {
        return ResponseEntity.ok(returnService.getReturnByDebitNoteNumber(
                context.getTenantId(), context.getCompanyId(), debitNoteNumber));
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseReturnResponse>> findAll(
            @RequestParam(required = false) PurchaseReturnStatus status,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "returnDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Long tenantId = context.getTenantId();
        Long companyId = context.getCompanyId();

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(returnService.searchReturns(tenantId, companyId, search.trim(), pageable));
        }
        if (invoiceId != null) {
            return ResponseEntity.ok(returnService.listReturnsByInvoice(tenantId, companyId, invoiceId, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(returnService.listReturnsByStatus(tenantId, companyId, status, pageable));
        }
        return ResponseEntity.ok(returnService.listReturns(tenantId, companyId, pageable));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseReturnResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.approveReturn(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<PurchaseReturnResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.postReturn(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseReturnResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody ReturnDecisionRequest request
    ) {
        return ResponseEntity.ok(returnService.cancelReturn(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @PostMapping("/{id}/retry-stock")
    public ResponseEntity<PurchaseReturnResponse> retryStockPost(@PathVariable Long id) {
        return ResponseEntity.ok(returnService.retryStockPost(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @GetMapping("/invoices/{invoiceId}/returnable-lines")
    public ResponseEntity<List<ReturnableLineResponse>> getReturnableLines(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(returnService.getReturnableLinesForInvoice(
                context.getTenantId(), context.getCompanyId(), invoiceId));
    }
}

