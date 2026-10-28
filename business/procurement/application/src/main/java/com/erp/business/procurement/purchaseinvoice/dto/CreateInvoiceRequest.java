package com.erp.business.procurement.purchaseinvoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Records a supplier's invoice against a delivery that has been received.
 *
 * <p><strong>Prices are taken from the supplier, not the order.</strong> That is the
 * whole point of a three-way match: the amount owed is what the supplier says they
 * charged, and comparing it against the order is what surfaces a discrepancy. Copying the
 * ordered price here would make a mismatch impossible to see.
 *
 * <p><strong>Quantities default to what was received.</strong> They are accepted so a
 * supplier billing a short or over delivery can be recorded as billed rather than
 * corrected, but a request that omits them is billed for exactly what arrived.
 */
public record CreateInvoiceRequest(

        @NotNull(message = "Goods receipt is required")
        @Positive(message = "Goods receipt id must be positive")
        Long goodsReceiptId,

        /** The supplier's own invoice number. */
        @NotBlank(message = "Supplier invoice number is required")
        @Size(max = 60)
        String invoiceNumber,

        /** When the supplier issued it. Defaults to today. */
        LocalDate invoiceDate,

        /** When payment falls due. */
        LocalDate dueDate,

        @NotEmpty(message = "At least one invoice line is required")
        @Valid
        List<Line> lines,

        @Size(max = 2000)
        String notes
) {

    /**
     * One billed line.
     *
     * <p>The quantity and unit price are optional: omitted, they are taken from the
     * matching receipt line. An invoice that is a faithful record of the delivery needs
     * nothing but the receipt id and the invoice number.
     */
    public record Line(

            @NotNull(message = "Receipt line is required")
            @Positive(message = "Receipt line id must be positive")
            Long goodsReceiptLineId,

            @DecimalMin(value = "0.000001", message = "Quantity must be greater than zero")
            BigDecimal quantity,

            @DecimalMin(value = "0", message = "Unit price cannot be negative")
            BigDecimal unitPrice,

            @DecimalMin(value = "0", message = "Discount cannot be below zero")
            @DecimalMax(value = "100", message = "Discount cannot exceed 100")
            BigDecimal discountPercentage,

            @DecimalMin(value = "0", message = "Tax rate cannot be below zero")
            @DecimalMax(value = "100", message = "Tax rate cannot exceed 100")
            BigDecimal taxRate,

            @Size(max = 500)
            String description
    ) {
    }
}
