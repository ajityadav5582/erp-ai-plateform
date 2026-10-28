package com.erp.business.sales.cbms;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.domain.SalesReturn;
import com.erp.business.sales.salesreturn.infrastructure.persistence.SalesReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CbmsSyncWorker {

    private final SalesInvoiceRepository invoiceRepository;
    private final SalesReturnRepository returnRepository;
    private final CbmsClient cbmsClient;

    /**
     * Outbox scheduler: runs every 30 seconds to push un-synced bills and credit notes
     * to Nepal IRD's Central Billing Monitoring System (CBMS).
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processOutbox() {
        processInvoices();
        processReturns();
    }

    private void processInvoices() {
        List<SalesInvoice> pendingInvoices = invoiceRepository.findByCbmsSyncedFalseAndStatusIn(
                List.of(SalesInvoiceStatus.ISSUED, SalesInvoiceStatus.PARTIALLY_PAID, SalesInvoiceStatus.PAID)
        );

        if (!pendingInvoices.isEmpty()) {
            log.info("CBMS Outbox: found {} un-synced sales invoice(s)", pendingInvoices.size());
            for (SalesInvoice inv : pendingInvoices) {
                syncInvoice(inv);
            }
        }
    }

    private void processReturns() {
        List<SalesReturn> pendingReturns = returnRepository.findByCbmsSyncedFalseAndStatus(
                ReturnStatus.POSTED
        );

        if (!pendingReturns.isEmpty()) {
            log.info("CBMS Outbox: found {} un-synced sales return(s)", pendingReturns.size());
            for (SalesReturn ret : pendingReturns) {
                syncReturn(ret);
            }
        }
    }

    @Transactional
    public CbmsSyncResult syncInvoice(SalesInvoice invoice) {
        CbmsBillPayload payload = new CbmsBillPayload(
                cbmsClient.getUsername(),
                cbmsClient.getPassword(),
                "123456789", // Seller PAN (configured or company pan)
                invoice.getCustomerPanNumber() != null ? invoice.getCustomerPanNumber() : "",
                invoice.getCustomerName(),
                "2081/82", // Nepalese fiscal year
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDateNp() != null ? invoice.getInvoiceDateNp() : invoice.getInvoiceDate().toString(),
                invoice.getTotalAmount(),
                BigDecimal.ZERO,
                invoice.getTaxableAmount(),
                invoice.getVatAmount(),
                invoice.getExemptAmount(),
                true
        );

        CbmsSyncResult result = cbmsClient.postBill(payload);
        if (result.success()) {
            invoice.markCbmsSynced(result.responseBody());
        } else {
            invoice.recordCbmsSyncFailure(result.message());
        }
        invoiceRepository.save(invoice);
        return result;
    }

    @Transactional
    public CbmsSyncResult syncReturn(SalesReturn salesReturn) {
        CbmsBillReturnPayload payload = new CbmsBillReturnPayload(
                cbmsClient.getUsername(),
                cbmsClient.getPassword(),
                "123456789", // Seller PAN
                salesReturn.getCustomerPanNumber() != null ? salesReturn.getCustomerPanNumber() : "",
                "2081/82",
                salesReturn.getInvoiceNumber(),
                salesReturn.getCreditNoteNumber(),
                salesReturn.getReturnDateNp() != null ? salesReturn.getReturnDateNp() : salesReturn.getReturnDate().toString(),
                salesReturn.getReason() != null ? salesReturn.getReason() : "Goods returned",
                salesReturn.getTotalAmount(),
                salesReturn.getSubtotalAmount(),
                salesReturn.getVatAmount()
        );

        CbmsSyncResult result = cbmsClient.postBillReturn(payload);
        if (result.success()) {
            salesReturn.markCbmsSynced(result.responseBody());
        } else {
            salesReturn.recordCbmsSyncFailure(result.message());
        }
        returnRepository.save(salesReturn);
        return result;
    }
}
