package com.erp.business.reports.vat.annex10;

import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/vat/annex-10")
@RequiredArgsConstructor
public class VatReturnController {

    private final VatReturnService service;
    private final BusinessContextAccessor context;

    @GetMapping
    public ResponseEntity<VatReturnResponse> get(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long fiscalYearId,
            @RequestParam int nepaliMonth) {
        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        Long fy = fiscalYearId != null ? fiscalYearId : context.getFiscalYearId();
        return ResponseEntity.ok(service.compute(tenantId, cid, fy, nepaliMonth));
    }

    @GetMapping("/export-pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long fiscalYearId,
            @RequestParam int nepaliMonth) {
        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        Long fy = fiscalYearId != null ? fiscalYearId : context.getFiscalYearId();
        byte[] bytes = service.exportPdf(tenantId, cid, fy, nepaliMonth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=annex-10.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
