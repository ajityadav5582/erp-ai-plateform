package com.erp.business.sales.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreatePosInvoiceRequest(
        @NotNull Long posTransactionId,
        Long customerId,
        String customerName,
        String customerPanNumber,
        @NotNull LocalDate invoiceDate,
        String notes,
        @NotEmpty @Valid List<InvoiceLineRequest> lines
) {
}
