package com.erp.business.procurement.supplierpayment.dto;

import java.math.BigDecimal;

/**
 * Line-level allocation returned to clients.
 */
public record PaymentAllocationResponse(
        Long id,
        Long purchaseInvoiceId,
        String invoiceNumber,
        BigDecimal allocatedAmount,
        BigDecimal discountAmount,
        String notes
) {
}

