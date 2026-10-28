package com.erp.business.sales.receipt.dto;

import com.erp.business.sales.receipt.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to record a customer receipt against an invoice.
 *
 * <p>Tenant, company and the receipt number are supplied by the service from the
 * request context and the document counter, never from the body. The customer
 * snapshot is copied from the invoice.
 */
public record CreateReceiptRequest(

        @NotNull(message = "Invoice is required")
        Long invoiceId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 4, message = "Amount has too many digits")
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Receipt date is required")
        LocalDate receiptDate,

        @Size(max = 100, message = "Reference must not exceed 100 characters")
        String reference,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
