package com.erp.business.sales.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to create and issue a direct sales (tax) invoice without requiring a pre-existing SalesOrder.
 */
public record CreateDirectSalesInvoiceRequest(

        Long customerId,

        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String customerName,

        @Size(max = 16, message = "Customer PAN must not exceed 16 characters")
        String customerPanNumber,

        Long warehouseId,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        LocalDate dueDate,

        Boolean isSettled,

        String paymentMethod,

        String paymentReference,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotEmpty(message = "At least one invoice line is required")
        @Valid
        List<InvoiceLineRequest> lines
) {
}

