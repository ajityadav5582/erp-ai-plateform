package com.erp.business.sales.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateSalesQuotationRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Quotation date is required")
        LocalDate quotationDate,

        LocalDate validUntilDate,

        Long branchId,

        Long warehouseId,

        String reference,

        String termsAndConditions,

        String notes,

        @NotEmpty(message = "At least one quotation line is required")
        @Valid
        List<SalesQuotationLineRequest> lines
) {
}
