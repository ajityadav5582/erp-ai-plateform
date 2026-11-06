package com.erp.business.pos.reports.creditnotes;

import java.math.BigDecimal;

public record CreditNoteRow(
        String returnDateBs,
        String creditNoteNumber,
        String originalInvoiceNumber,
        String buyerName,
        String buyerPan,
        BigDecimal returnAmount,
        BigDecimal taxableReturnAmount,
        BigDecimal vatDeducted,
        String reason,
        String cbmsStatus
) {
}
