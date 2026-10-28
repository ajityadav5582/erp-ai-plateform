package com.erp.business.procurement.supplierpayment.dto;

import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pays a supplier against an invoice.
 *
 * <p>The amount is required rather than defaulting to the whole balance, because
 * settling in instalments is ordinary and a payment of "the balance" is rarely what a
 * treasury clerk means when a part payment is overdue.
 *
 * <p>Supplier and currency are not asked for: they belong to the invoice being settled,
 * and taking them from the caller would allow a payment to be recorded against an invoice
 * of a different currency.
 */
public record CreatePaymentRequest(

        @NotNull(message = "Purchase invoice is required")
        @Positive(message = "Purchase invoice id must be positive")
        Long purchaseInvoiceId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        /** When the money moved. Defaults to today. */
        LocalDate paymentDate,

        /** Cheque number or bank transaction reference. */
        @Size(max = 100)
        String referenceNo,

        /** The branch or account the money left from, for bank reconciliation. */
        @Size(max = 255)
        String paidFrom,

        @Size(max = 2000)
        String notes
) {
}
