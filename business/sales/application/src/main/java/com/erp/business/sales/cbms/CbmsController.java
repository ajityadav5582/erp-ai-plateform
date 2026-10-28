package com.erp.business.sales.cbms;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.salesreturn.domain.SalesReturn;
import com.erp.business.sales.salesreturn.infrastructure.persistence.SalesReturnRepository;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.business.sales.shared.security.SalesInvoicePermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales/cbms")
@RequiredArgsConstructor
public class CbmsController {

    private final CbmsSyncWorker syncWorker;
    private final SalesInvoiceRepository invoiceRepository;
    private final SalesReturnRepository returnRepository;
    private final BusinessContextAccessor context;

    @PostMapping("/invoices/{id}/sync")
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public ResponseEntity<CbmsSyncResult> syncInvoice(@PathVariable Long id) {
        SalesInvoice invoice = invoiceRepository
                .findByTenantIdAndCompanyIdAndId(context.getTenantId(), context.getCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + id));

        CbmsSyncResult result = syncWorker.syncInvoice(invoice);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/returns/{id}/sync")
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public ResponseEntity<CbmsSyncResult> syncReturn(@PathVariable Long id) {
        SalesReturn salesReturn = returnRepository
                .findByTenantIdAndCompanyIdAndId(context.getTenantId(), context.getCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Sales return not found with ID: " + id));

        CbmsSyncResult result = syncWorker.syncReturn(salesReturn);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public ResponseEntity<Map<String, Object>> getStatus() {
        int pendingInvoices = invoiceRepository
                .findByTenantIdAndCompanyIdAndCbmsSyncedFalse(context.getTenantId(), context.getCompanyId()).size();
        int pendingReturns = returnRepository
                .findByTenantIdAndCompanyIdAndCbmsSyncedFalse(context.getTenantId(), context.getCompanyId()).size();

        return ResponseEntity.ok(Map.of(
                "pendingInvoices", pendingInvoices,
                "pendingReturns", pendingReturns,
                "totalPending", pendingInvoices + pendingReturns
        ));
    }
}
