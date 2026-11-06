package com.erpai.integration.ird.port;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FiscalBillCommand(
        Long tenantId,
        Long companyId,
        String invoiceNumber,
        String fiscalYear,
        String sellerPan,
        String buyerPan,
        String buyerName,
        BigDecimal totalAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal exemptAmount,
        boolean realTime,
        LocalDate billDate
) {
    public FiscalBillCommand withRealtime(boolean realTime) {
        return new FiscalBillCommand(
                tenantId, companyId, invoiceNumber, fiscalYear, sellerPan, buyerPan, buyerName,
                totalAmount, taxableAmount, taxAmount, exemptAmount, realTime, billDate);
    }
}
