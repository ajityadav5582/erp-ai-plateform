package com.erp.business.sales.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload to create a sales delivery against a confirmed sales order.
 *
 * <p>Tenant, company and the allocated delivery number are supplied by the service
 * from the request context and the document counter, never from the body. The
 * customer snapshot is copied from the order, so the caller names only the order
 * and the warehouse the goods leave from.
 */
public record CreateDeliveryRequest(

        @NotNull(message = "Sales order is required")
        Long salesOrderId,

        @NotNull(message = "Warehouse is required")
        Long warehouseId,

        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        @NotEmpty(message = "At least one delivery line is required")
        @Valid
        List<DeliveryLineRequest> lines
) {
}
