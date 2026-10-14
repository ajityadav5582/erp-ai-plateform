package com.erp.business.sales.salesreturn;

import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.dto.CreateReturnRequest;
import com.erp.business.sales.salesreturn.dto.ReturnDecisionRequest;
import com.erp.business.sales.salesreturn.dto.SalesReturnResponse;
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
 * Sales return (credit note) endpoints.
 *
 * <p>Authorisation lives on the service, not here: every method carries
 * {@code @RequiresPermission}, which the shared aspect enforces before the body
 * runs. The controller only reads the authenticated tenant, company and user, so a
 * caller cannot widen its own scope through the payload.
 */
@RestController
@RequestMapping("/api/v1/sales/returns")
@RequiredArgsConstructor
public class SalesReturnController {

    private static final String BASE_PATH = "/api/v1/sales/returns";

    private final SalesReturnService salesReturnService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SalesReturnResponse> createReturn(
            @Valid @RequestBody CreateReturnRequest request,
            UriComponentsBuilder uriBuilder) {

        SalesReturnResponse response = salesReturnService.createReturn(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SalesReturnResponse>> listReturns(
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesReturnService.listReturns(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SalesReturnResponse>> searchReturns(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesReturnService.searchReturns(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/by-status")
    public ResponseEntity<Page<SalesReturnResponse>> listReturnsByStatus(
            @RequestParam("status") ReturnStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesReturnService.listReturnsByStatus(
                context.getTenantId(), context.getCompanyId(), status, pageable));
    }

    @GetMapping("/by-invoice")
    public ResponseEntity<Page<SalesReturnResponse>> listReturnsByInvoice(
            @RequestParam("invoiceId") Long invoiceId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(salesReturnService.listReturnsByInvoice(
                context.getTenantId(), context.getCompanyId(), invoiceId, pageable));
    }

    @GetMapping("/by-number")
    public ResponseEntity<SalesReturnResponse> getReturnByNumber(
            @RequestParam("creditNoteNumber") String creditNoteNumber) {
        return ResponseEntity.ok(salesReturnService.getReturnByNumber(
                context.getTenantId(), context.getCompanyId(), creditNoteNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesReturnResponse> getReturnById(@PathVariable Long id) {
        return ResponseEntity.ok(salesReturnService.getReturnById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<SalesReturnResponse> postReturn(@PathVariable Long id) {
        return ResponseEntity.ok(salesReturnService.postReturn(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesReturnResponse> cancelReturn(
            @PathVariable Long id, @Valid @RequestBody ReturnDecisionRequest request) {
        return ResponseEntity.ok(salesReturnService.cancelReturn(
                context.getTenantId(), context.getCompanyId(), id, request));
    }
}
