package com.erp.business.sales.register.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The sales register for a period: the figures a Nepal VAT return (VAT-201) is
 * filed from, net of credit notes.
 *
 * <p>Sales are the issued invoices in the period; credits are the posted credit
 * notes. The net figures are what the return reports: net taxable sales drive the
 * output-VAT liability, and exempt / zero-rated sales are declared separately.
 *
 * @param periodFrom       start of the reporting period (inclusive)
 * @param periodTo         end of the reporting period (inclusive)
 * @param invoiceCount     number of issued invoices in the period
 * @param creditNoteCount  number of posted credit notes in the period
 * @param taxableSales     gross standard-rated sales
 * @param exemptSales      gross exempt sales
 * @param zeroRatedSales   gross zero-rated (incl. export) sales
 * @param outputVat        gross output VAT on sales
 * @param creditTaxable    standard-rated sales reversed by credit notes
 * @param creditVat        output VAT reversed by credit notes
 * @param netTaxableSales  taxableSales - creditTaxable
 * @param netOutputVat     outputVat - creditVat
 */
public record SalesRegisterResponse(
        LocalDate periodFrom,
        LocalDate periodTo,
        long invoiceCount,
        long creditNoteCount,
        BigDecimal taxableSales,
        BigDecimal exemptSales,
        BigDecimal zeroRatedSales,
        BigDecimal outputVat,
        BigDecimal creditTaxable,
        BigDecimal creditVat,
        BigDecimal netTaxableSales,
        BigDecimal netOutputVat
) {
}
