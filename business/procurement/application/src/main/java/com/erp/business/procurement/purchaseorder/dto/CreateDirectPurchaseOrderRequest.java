package com.erp.business.procurement.purchaseorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Direct purchase order request without an upstream requisition.
 */
public record CreateDirectPurchaseOrderRequest(
        @NotNull(message = "Supplier is required")
        Long supplierId,

        String supplierCode,
        String supplierName,

        LocalDate orderDate,
        LocalDate expectedDeliveryDate,

        Long branchId,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,

        String currency,
        String reference,
        String notes,

        @NotEmpty(message = "At least one line item is required")
        @Valid
        List<Line> lines
) {
    public record Line(
            @NotNull(message = "Item is required")
            Long itemId,

            String itemCode,
            String itemName,
            String uomCode,
            Long unitId,

            @NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be greater than zero")
            BigDecimal quantity,

            @NotNull(message = "Unit price is required")
            @PositiveOrZero(message = "Unit price cannot be negative")
            BigDecimal unitPrice,

            BigDecimal discountPercentage,
            BigDecimal taxRate,
            LocalDate expectedDeliveryDate,
            String notes
    ) {}
}

