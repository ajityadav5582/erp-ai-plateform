package com.erp.business.procurement.supplierpayment.dto;

import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pays a supplier against one or more invoices.
 *
 * <p>Supports both:
 * <ul>
 *   <li>Single invoice settlement (legacy/convenience): supply {@code purchaseInvoiceId} and {@code amount}.</li>
 *   <li>Multi-invoice settlement: supply {@code supplierId}, {@code amount}, and a list of {@code allocations}.</li>
 * </ul>
 */
public record CreatePaymentRequest(

        /** Legacy/shortcut: single invoice being settled. Null for multi-invoice payments. */
        @Positive(message = "Purchase invoice id must be positive")
        Long purchaseInvoiceId,

        /** Supplier being paid. Required when making multi-invoice payments. */
        @Positive(message = "Supplier id must be positive")
        Long supplierId,

        /** Total money moving from bank/cash account. */
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
        String notes,

        /** Line-level invoice settlements for multi-invoice payments. */
        @Valid
        List<PaymentAllocationRequest> allocations
) {

    /**
     * Backward-compatible constructor for single-invoice payments.
     */
    public CreatePaymentRequest(
            Long purchaseInvoiceId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            LocalDate paymentDate,
            String referenceNo,
            String paidFrom,
            String notes) {
        this(purchaseInvoiceId, null, amount, paymentMethod, paymentDate, referenceNo, paidFrom, notes, null);
    }
}
