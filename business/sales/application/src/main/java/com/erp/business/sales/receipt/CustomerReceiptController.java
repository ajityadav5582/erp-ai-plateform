package com.erp.business.sales.receipt;

import com.erp.business.sales.receipt.domain.ReceiptStatus;
import com.erp.business.sales.receipt.dto.CreateReceiptRequest;
import com.erp.business.sales.receipt.dto.CustomerReceiptResponse;
import com.erp.business.sales.receipt.dto.ReceiptDecisionRequest;
import com.erp.business.sales.shared.context.BusinessContextAccessor;
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
 * Customer receipt endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/receipts")
@RequiredArgsConstructor
public class CustomerReceiptController {

    private static final String BASE_PATH = "/api/v1/sales/receipts";

    private final CustomerReceiptService customerReceiptService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<CustomerReceiptResponse> createReceipt(
            @Valid @RequestBody CreateReceiptRequest request,
            UriComponentsBuilder uriBuilder) {

        CustomerReceiptResponse response = customerReceiptService.createReceipt(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerReceiptResponse>> listReceipts(
            @PageableDefault(size = 20, sort = "receiptDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(customerReceiptService.listReceipts(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerReceiptResponse>> searchReceipts(
            @RequestParam("term") String term, Pageable pageable) {
        return ResponseEntity.ok(customerReceiptService.searchReceipts(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<CustomerReceiptResponse>> listReceiptsByStatus(
            @RequestParam("status") ReceiptStatus status, Pageable pageable) {
        return ResponseEntity.ok(customerReceiptService.listReceiptsByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-invoice")
    public ResponseEntity<Page<CustomerReceiptResponse>> listReceiptsByInvoice(
            @RequestParam("invoiceId") Long invoiceId, Pageable pageable) {
        return ResponseEntity.ok(customerReceiptService.listReceiptsByInvoice(
                context.getTenantId(), context.getCompanyId(), invoiceId, pageable));
    }

    @GetMapping("/by-customer")
    public ResponseEntity<Page<CustomerReceiptResponse>> listReceiptsByCustomer(
            @RequestParam("customerId") Long customerId, Pageable pageable) {
        return ResponseEntity.ok(customerReceiptService.listReceiptsByCustomer(
                context.getTenantId(), context.getCompanyId(), customerId, pageable));
    }

    @GetMapping("/by-number")
    public ResponseEntity<CustomerReceiptResponse> getReceiptByNumber(
            @RequestParam("receiptNumber") String receiptNumber) {
        return ResponseEntity.ok(customerReceiptService.getReceiptByNumber(
                context.getTenantId(), context.getCompanyId(), receiptNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerReceiptResponse> getReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(customerReceiptService.getReceiptById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CustomerReceiptResponse> cancelReceipt(
            @PathVariable Long id, @Valid @RequestBody ReceiptDecisionRequest request) {
        return ResponseEntity.ok(customerReceiptService.cancelReceipt(
                context.getTenantId(), context.getCompanyId(), id, request));
    }
}
