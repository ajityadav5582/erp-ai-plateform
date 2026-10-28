package com.erp.business.sales.salesreturn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to raise a sales return (credit note) against an invoice.
 *
 * <p>Tenant, company and the credit-note number are supplied by the service from
 * the request context and the document counter, never from the body. The customer
 * snapshot is copied from the invoice.
 */
public record CreateReturnRequest(

        @NotNull(message = "Invoice is required")
        Long invoiceId,

        @NotNull(message = "Warehouse is required")
        Long warehouseId,

        @NotNull(message = "Return date is required")
        LocalDate returnDate,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @NotEmpty(message = "At least one return line is required")
        @Valid
        List<ReturnLineRequest> lines
) {
}
