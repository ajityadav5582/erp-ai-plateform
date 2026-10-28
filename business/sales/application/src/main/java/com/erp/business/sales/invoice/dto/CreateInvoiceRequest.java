package com.erp.business.sales.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to create a sales (tax) invoice against a sales order.
 *
 * <p>Tenant, company and the IRD invoice number are supplied by the service from
 * the request context and the document counter, never from the body. The customer
 * snapshot (including PAN) is copied from the order.
 */
public record CreateInvoiceRequest(

        @NotNull(message = "Sales order is required")
        Long salesOrderId,

        Long deliveryId,

        @NotNull(message = "Invoice date is required")
        LocalDate invoiceDate,

        LocalDate dueDate,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotEmpty(message = "At least one invoice line is required")
        @Valid
        List<InvoiceLineRequest> lines
) {
}
