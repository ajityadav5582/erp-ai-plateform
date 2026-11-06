package com.erp.business.procurement.reports.vatreconciliation;

import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/vat/reconciliation-summary")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService service;
    private final BusinessContextAccessor context;

    @GetMapping
    public ResponseEntity<ReconciliationSummary> get(
            @RequestParam(required = false) Long companyId,
            @RequestParam int bsYear,
            @RequestParam int bsMonth) {
        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        return ResponseEntity.ok(service.getSummary(tenantId, cid, bsYear, bsMonth));
    }
}
