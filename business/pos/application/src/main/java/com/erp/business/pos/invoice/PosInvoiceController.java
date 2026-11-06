package com.erp.business.pos.invoice;

import com.erp.business.pos.domain.PosInvoice;
import com.erp.business.pos.invoice.dto.CreatePosInvoiceRequest;
import com.erp.platform.business.context.BusinessContextAccessor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/invoices")
public class PosInvoiceController {

    private final PosInvoiceService invoiceService;
    private final BusinessContextAccessor context;

    public PosInvoiceController(PosInvoiceService invoiceService, BusinessContextAccessor context) {
        this.invoiceService = invoiceService;
        this.context = context;
    }

    @PostMapping
    public ResponseEntity<PosInvoice> createInvoice(@Valid @RequestBody CreatePosInvoiceRequest request) {
        PosInvoice invoice = invoiceService.createInvoice(
                context.getTenantId(), context.getCompanyId(), request);
        return ResponseEntity.ok(invoice);
    }
}
