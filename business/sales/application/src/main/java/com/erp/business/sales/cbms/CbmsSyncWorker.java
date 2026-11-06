package com.erp.business.sales.cbms;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.salesreturn.domain.SalesReturn;
import com.erp.business.sales.salesreturn.infrastructure.persistence.SalesReturnRepository;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.erpai.integration.ird.port.IrdSyncResult;
import com.erpai.integration.ird.port.out.IrdFiscalSyncPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retired: Sales invoices and credit notes are now reported to IRD exclusively
 * through the central IRD outbox. This worker no longer submits independently,
 * to avoid duplicate reporting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CbmsSyncWorker {

    private final SalesInvoiceRepository invoiceRepository;
    private final SalesReturnRepository returnRepository;

    @Transactional
    public CbmsSyncResult syncInvoice(SalesInvoice invoice) {
        log.info("Sales CBMS sync retired; invoice {} is handled by the central IRD outbox.",
                invoice.getInvoiceNumber());
        return CbmsSyncResult.ok("Handled by central IRD outbox");
    }

    @Transactional
    public CbmsSyncResult syncReturn(SalesReturn salesReturn) {
        log.info("Sales CBMS sync retired; return {} is handled by the central IRD outbox.",
                salesReturn.getCreditNoteNumber());
        return CbmsSyncResult.ok("Handled by central IRD outbox");
    }
}
