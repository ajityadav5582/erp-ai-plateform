package com.erp.business.procurement.reports.vatreconciliation;

import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.platform.common.nepali.date.NepaliDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ReconciliationServiceImpl implements ReconciliationService {

    private final PurchaseInvoiceRepository repository;

    public ReconciliationServiceImpl(PurchaseInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReconciliationSummary getSummary(Long tenantId, Long companyId, int bsYear, int bsMonth) {
        NepaliDate first = new NepaliDate(bsYear, bsMonth, 1);
        NepaliDate last = new NepaliDate(bsYear, bsMonth, first.lengthOfMonth());
        LocalDate from = first.toAdDate();
        LocalDate to = last.toAdDate();

        BigDecimal inputVat = BigDecimal.ZERO;
        var page = repository.findPurchaseBook(tenantId, companyId, null, null, from, to, PageRequest.of(0, Integer.MAX_VALUE));
        for (Object[] r : page.getContent()) {
            Object tax = r[6];
            if (tax != null) inputVat = inputVat.add(new BigDecimal(tax.toString()));
        }

        BigDecimal outputVat = BigDecimal.ZERO; // Sales book (Annex 5) aggregation lives in sales module
        BigDecimal creditNoteVat = BigDecimal.ZERO; // Annex 6
        BigDecimal net = outputVat.subtract(inputVat).subtract(creditNoteVat);
        return new ReconciliationSummary(bsYear, bsMonth, outputVat, inputVat, creditNoteVat, net);
    }
}
