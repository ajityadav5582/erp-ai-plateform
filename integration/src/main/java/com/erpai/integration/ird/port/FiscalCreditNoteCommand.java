package com.erpai.integration.ird.port;

import java.math.BigDecimal;

public record FiscalCreditNoteCommand(
        Long tenantId,
        Long companyId,
        String creditNoteNumber,
        String creditNoteDateBs,
        String refInvoiceNumber,
        String fiscalYearCode,
        String customerPan,
        String customerName,
        BigDecimal totalAmount,
        BigDecimal taxableAmount,
        BigDecimal vatAmount,
        BigDecimal exemptAmount,
        BigDecimal excisableAmount,
        String reason,
        boolean isRealtime
) {
    public FiscalCreditNoteCommand withRealtime(boolean realTime) {
        return new FiscalCreditNoteCommand(
                tenantId, companyId, creditNoteNumber, creditNoteDateBs, refInvoiceNumber, fiscalYearCode,
                customerPan, customerName, totalAmount, taxableAmount, vatAmount, exemptAmount,
                excisableAmount, reason, realTime);
    }
}
