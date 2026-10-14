package com.erp.business.sales.quotation;

import com.erp.business.sales.quotation.domain.SalesQuotationStatus;
import com.erp.business.sales.quotation.dto.CreateSalesQuotationRequest;
import com.erp.business.sales.quotation.dto.SalesQuotationResponse;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales/quotations")
@RequiredArgsConstructor
public class SalesQuotationController {

    private static final String BASE_PATH = "/api/v1/sales/quotations";

    private final SalesQuotationService quotationService;
    private final BusinessContextAccessor context;

    @PostMapping
    public ResponseEntity<SalesQuotationResponse> createQuotation(
            @Valid @RequestBody CreateSalesQuotationRequest request,
            UriComponentsBuilder uriBuilder) {
        SalesQuotationResponse response = quotationService.createQuotation(
                context.getTenantId(), context.getCompanyId(), request);

        return ResponseEntity
                .created(uriBuilder.path(BASE_PATH + "/{id}").buildAndExpand(response.id()).toUri())
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SalesQuotationResponse>> listQuotations(
            @RequestParam(value = "status", required = false) SalesQuotationStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(quotationService.listQuotationsByStatus(
                    context.getTenantId(), context.getCompanyId(), status, pageable));
        }
        return ResponseEntity.ok(quotationService.listQuotations(
                context.getTenantId(), context.getCompanyId(), pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SalesQuotationResponse>> searchQuotations(
            @RequestParam("term") String term,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(quotationService.searchQuotations(
                context.getTenantId(), context.getCompanyId(), term, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesQuotationResponse> getQuotationById(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.getQuotationById(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<SalesQuotationResponse> sendQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.sendQuotation(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<SalesQuotationResponse> acceptQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.acceptQuotation(
                context.getTenantId(), context.getCompanyId(), id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SalesQuotationResponse> rejectQuotation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        return ResponseEntity.ok(quotationService.rejectQuotation(
                context.getTenantId(), context.getCompanyId(), id, reason));
    }

    @PostMapping("/{id}/convert-to-order")
    public ResponseEntity<SalesOrderResponse> convertToOrder(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.convertToSalesOrder(
                context.getTenantId(), context.getCompanyId(), id));
    }
}
