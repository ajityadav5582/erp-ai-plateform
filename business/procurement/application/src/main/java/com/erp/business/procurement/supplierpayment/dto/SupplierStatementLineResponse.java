package com.erp.business.procurement.supplierpayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single chronological transaction line in the supplier's running statement of account.
 */
public record SupplierStatementLineResponse(
        LocalDate date,
        String documentType,      // "INVOICE", "PAYMENT", "DEBIT_NOTE"
        Long documentId,
        String documentNumber,    // e.g. "INV-2026-001" or "PAY-0001" or "DN-0001"
        String referenceNumber,   // supplier's own bill # or external ref
        String description,
        BigDecimal debitAmount,   // payments & debit notes reduce payable
        BigDecimal creditAmount,  // invoices increase payable
        BigDecimal runningBalance
) {
}

