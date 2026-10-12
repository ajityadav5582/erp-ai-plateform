package com.erp.business.sales.salesorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to update a draft sales order.
 *
 * <p>Only orders in DRAFT status can be updated. Replaces editable header fields
 * and synchronizes the line items.
 */
public record UpdateSalesOrderRequest(

        @NotNull(message = "Customer is required")
        Long customerId,

        @NotNull(message = "Order date is required")
        LocalDate orderDate,

        LocalDate expectedDeliveryDate,

        Long branchId,

        Long warehouseId,

        @PositiveOrZero(message = "Payment terms days must not be negative")
        Integer paymentTermsDays,

        @Size(max = 100, message = "Reference must not exceed 100 characters")
        String reference,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotEmpty(message = "At least one order line is required")
        @Valid
        List<SalesOrderLineRequest> lines
) {
}
