package com.erp.business.procurement.purchaseinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Fast-match request: records both goods receipt and supplier tax invoice in one
 * atomic transaction against an approved purchase order when delivery and bill arrive together.
 */
public record FastMatchPoRequest(
        @NotNull(message = "Purchase order ID is required")
        @Positive(message = "Purchase order ID must be positive")
        Long purchaseOrderId,

        @NotBlank(message = "Supplier invoice number is required")
        @Size(max = 60, message = "Invoice number must not exceed 60 characters")
        String invoiceNumber,

        LocalDate invoiceDate,
        LocalDate dueDate,

        Long warehouseId,
        String deliveryChallanNo,
        String notes
) {
}

