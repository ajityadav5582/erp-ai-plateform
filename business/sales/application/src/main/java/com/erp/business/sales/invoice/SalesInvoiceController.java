package com.erp.business.sales.invoice;

import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.dto.CreateInvoiceRequest;
import com.erp.business.sales.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceDecisionRequest;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.invoice.dto.TaxInvoiceView;
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
 * Sales (tax) invoice endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/invoices")
@RequiredArgsConstructor
public class SalesInvoiceController {

    private static final String BASE_PATH = "/api/v1/sales/invoices";

    private final SalesInvoiceService salesInvoiceService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SalesInvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            UriComponentsBuilder uriBuilder) {

        SalesInvoiceResponse response = salesInvoiceService.createInvoice(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @PostMapping("/pos")
    public ResponseEntity<SalesInvoiceResponse> createPosInvoice(
            @Valid @RequestBody CreatePosInvoiceRequest request,
            UriComponentsBuilder uriBuilder) {

        SalesInvoiceResponse response = salesInvoiceService.createPosInvoice(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SalesInvoiceResponse>> listInvoices(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesInvoiceService.listInvoices(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SalesInvoiceResponse>> searchInvoices(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesInvoiceService.searchInvoices(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<SalesInvoiceResponse>> listInvoicesByStatus(
            @RequestParam("status") SalesInvoiceStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesInvoiceService.listInvoicesByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-customer")
    public ResponseEntity<Page<SalesInvoiceResponse>> listInvoicesByCustomer(
            @RequestParam("customerId") Long customerId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesInvoiceService.listInvoicesByCustomer(
                context.getTenantId(), context.getCompanyId(), customerId, pageable));
    }

    @GetMapping("/outstanding")
    public ResponseEntity<List<SalesInvoiceResponse>> listOutstandingInvoices() {
        return ResponseEntity.ok(salesInvoiceService.listOutstandingInvoices(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/invoiced-order-ids")
    public ResponseEntity<List<Long>> getInvoicedOrderIds() {
        return ResponseEntity.ok(salesInvoiceService.getInvoicedOrderIds(
                context.getTenantId(), context.getCompanyId()));
    }

    @GetMapping("/by-order")
    public ResponseEntity<SalesInvoiceResponse> getInvoiceByOrder(
            @RequestParam("salesOrderId") Long salesOrderId) {
        return ResponseEntity.ok(salesInvoiceService.getInvoiceByOrder(
                context.getTenantId(), context.getCompanyId(), salesOrderId));
    }

    @GetMapping("/by-number")
    public ResponseEntity<SalesInvoiceResponse> getInvoiceByNumber(
            @RequestParam("invoiceNumber") String invoiceNumber) {
        return ResponseEntity.ok(salesInvoiceService.getInvoiceByNumber(
                context.getTenantId(), context.getCompanyId(), invoiceNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesInvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(salesInvoiceService.getInvoiceById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<SalesInvoiceResponse> issueInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(salesInvoiceService.issueInvoice(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesInvoiceResponse> cancelInvoice(
            @PathVariable Long id, @Valid @RequestBody InvoiceDecisionRequest request) {
        return ResponseEntity.ok(salesInvoiceService.cancelInvoice(
                context.getTenantId(), context.getCompanyId(), id, request));
    }

    @GetMapping("/{id}/tax-invoice")
    public ResponseEntity<TaxInvoiceView> getTaxInvoiceView(@PathVariable Long id) {
        return ResponseEntity.ok(salesInvoiceService.getTaxInvoiceView(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
