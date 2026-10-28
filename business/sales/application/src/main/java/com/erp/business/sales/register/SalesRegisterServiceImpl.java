package com.erp.business.sales.register;

import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.register.dto.SalesRegisterResponse;
import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.infrastructure.persistence.SalesReturnRepository;
import com.erp.business.sales.shared.security.SalesInvoicePermissions;
import com.erp.platform.security.annotation.RequiresPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sales register implementation.
 *
 * <p>Sums the issued invoices and the posted credit notes in the period with two
 * aggregate queries, then nets the credits off the sales. Only settled-or-issued
 * invoices count as sales; drafts and cancelled invoices are excluded, as are
 * draft and cancelled credit notes.
 */
@Service
public class SalesRegisterServiceImpl implements SalesRegisterService {

    private final SalesInvoiceRepository invoiceRepository;
    private final SalesReturnRepository returnRepository;

    public SalesRegisterServiceImpl(SalesInvoiceRepository invoiceRepository,
                                    SalesReturnRepository returnRepository) {
        this.invoiceRepository = invoiceRepository;
        this.returnRepository = returnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(SalesInvoicePermissions.SALES_INVOICE_VIEW)
    public SalesRegisterResponse getRegister(Long tenantId, Long companyId, LocalDate from, LocalDate to) {
        Object[] sales = invoiceRepository.sumRegister(
                tenantId, companyId,
                List.of(SalesInvoiceStatus.ISSUED, SalesInvoiceStatus.PARTIALLY_PAID, SalesInvoiceStatus.PAID),
                from, to);
        Object[] credits = returnRepository.sumRegister(
                tenantId, companyId, ReturnStatus.POSTED, from, to);

        BigDecimal taxableSales = dec(sales[0]);
        BigDecimal exemptSales = dec(sales[1]);
        BigDecimal zeroRatedSales = dec(sales[2]);
        BigDecimal outputVat = dec(sales[3]);
        long invoiceCount = count(sales[4]);

        BigDecimal creditTaxable = dec(credits[0]);
        BigDecimal creditVat = dec(credits[1]);
        long creditNoteCount = count(credits[2]);

        return new SalesRegisterResponse(
                from, to, invoiceCount, creditNoteCount,
                taxableSales, exemptSales, zeroRatedSales, outputVat,
                creditTaxable, creditVat,
                taxableSales.subtract(creditTaxable),
                outputVat.subtract(creditVat));
    }

    private static BigDecimal dec(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }

    private static long count(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
