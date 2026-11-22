package com.erp.business.procurement.supplierpayment;

import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.procurement.supplierpayment.dto.ApplyAdvanceRequest;
import com.erp.business.procurement.supplierpayment.dto.CreatePaymentRequest;
import com.erp.business.procurement.supplierpayment.dto.SupplierPaymentResponse;
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

import java.util.List;

/**
 * Supplier payment endpoints.
 *
 * <p>Authorisation lives on the service: every method carries {@code @RequiresPermission},
 * enforced by the shared aspect before the body runs.
 */
@RestController
@RequestMapping("/api/v1/procurement/supplier-payments")
@RequiredArgsConstructor
public class SupplierPaymentController {

    private static final String BASE_PATH = "/api/v1/procurement/supplier-payments";

    private final SupplierPaymentService paymentService;
    private final BusinessContextAccessor context;

    /**
     * Pays a supplier against an invoice.
     *
     * <p>POST rather than PUT because paying is an event. Repeating it is refused by the
     * invoice's remaining balance rather than being idempotent, so it must not be
     * modelled as a replacement of the invoice.
     */
    @PostMapping
    public ResponseEntity<SupplierPaymentResponse> pay(
            @Valid @RequestBody CreatePaymentRequest request,
            UriComponentsBuilder uriBuilder) {

        SupplierPaymentResponse response = paymentService.pay(
                context.getTenantId(), context.getCompanyId(), request, currentUser());

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<SupplierPaymentResponse> getById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getById(
                context.getTenantId(), context.getCompanyId(), paymentId));
    }

    @GetMapping
    public ResponseEntity<Page<SupplierPaymentResponse>> findAll(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(paymentService.findAll(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    /** The payments made against one invoice. */
    @GetMapping("/by-invoice/{invoiceId}")
    public ResponseEntity<Page<SupplierPaymentResponse>> findByInvoice(
            @PathVariable Long invoiceId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(paymentService.findByInvoice(
                context.getTenantId(), context.getCompanyId(), invoiceId, pageable));
    }

    /** Available unallocated advance payments for a supplier. */
    @GetMapping("/advances/available")
    public ResponseEntity<List<SupplierPaymentResponse>> findAvailableAdvances(
            @RequestParam Long supplierId) {
        return ResponseEntity.ok(paymentService.findAvailableAdvances(
                context.getTenantId(), context.getCompanyId(), supplierId));
    }

    /** Applies available advance credit against an approved purchase invoice. */
    @PostMapping("/apply-advance")
    public ResponseEntity<SupplierPaymentResponse> applyAdvance(
            @Valid @RequestBody ApplyAdvanceRequest request) {
        return ResponseEntity.ok(paymentService.applyAdvance(
                context.getTenantId(), context.getCompanyId(), request, currentUser()));
    }

    /** Live balance summary (total outstanding payable & available advance credit) for a supplier. */
    @GetMapping("/suppliers/{supplierId}/balance-summary")
    public ResponseEntity<com.erp.business.procurement.supplierpayment.dto.SupplierBalanceSummaryResponse> getSupplierBalanceSummary(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(paymentService.getSupplierBalanceSummary(
                context.getTenantId(), context.getCompanyId(), supplierId));
    }

    /** Chronological running Statement of Account for a supplier. */
    @GetMapping("/suppliers/{supplierId}/statement")
    public ResponseEntity<com.erp.business.procurement.supplierpayment.dto.SupplierStatementResponse> getSupplierStatement(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate) {
        return ResponseEntity.ok(paymentService.getSupplierStatement(
                context.getTenantId(), context.getCompanyId(), supplierId, fromDate, toDate));
    }

    private String currentUser() {
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("An authenticated user is required for this operation");
        }
        return userId;
    }
}
