package com.erp.business.procurement.reports.purchasebook;

import com.erp.platform.business.context.BusinessContextAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/procurement/reports/purchase-book")
@RequiredArgsConstructor
public class PurchaseBookController {

    private final PurchaseBookService service;
    private final BusinessContextAccessor context;

    @GetMapping
    public ResponseEntity<PurchaseBookResponse> get(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long fiscalYearId,
            @RequestParam(required = false) String startDateNp,
            @RequestParam(required = false) String endDateNp,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("invoiceDate").descending());
        PurchaseBookResponse resp = service.getPurchaseBook(tenantId, cid, fiscalYearId, startDateNp, endDateNp, supplierId, pageable);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long fiscalYearId,
            @RequestParam(required = false) String startDateNp,
            @RequestParam(required = false) String endDateNp,
            @RequestParam(required = false) Long supplierId) {

        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        byte[] bytes = service.exportExcel(tenantId, cid, fiscalYearId, startDateNp, endDateNp, supplierId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-book-annex7.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export-csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long fiscalYearId,
            @RequestParam(required = false) String startDateNp,
            @RequestParam(required = false) String endDateNp,
            @RequestParam(required = false) Long supplierId) {

        Long tenantId = context.getTenantId();
        Long cid = companyId != null ? companyId : context.getCompanyId();
        String csv = service.exportCsv(tenantId, cid, fiscalYearId, startDateNp, endDateNp, supplierId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-book-annex7.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
