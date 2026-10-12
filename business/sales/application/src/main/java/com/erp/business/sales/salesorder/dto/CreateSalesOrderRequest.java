package com.erp.business.sales.salesorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to create a sales order.
 *
 * <p>Tenant, company and the allocated document number are supplied by the service
 * from the request context and the document counter, never from the body. At least
 * one line is required: an order with no lines cannot be confirmed.
 */
public record CreateSalesOrderRequest(

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
