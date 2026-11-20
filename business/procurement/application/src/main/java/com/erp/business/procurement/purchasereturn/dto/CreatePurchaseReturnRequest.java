package com.erp.business.procurement.purchasereturn.dto;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseReturnRequest(
        @NotNull(message = "Original purchase invoice ID is required")
        Long purchaseInvoiceId,

        Long warehouseId,

        LocalDate returnDate,

        @NotBlank(message = "A valid return reason is legally required under Nepal VAT Rule 23")
        String reason,

        String notes,

        @NotEmpty(message = "Purchase return must contain at least one line item")
        @Valid
        List<LineRequest> lines
) {
    public record LineRequest(
            Long purchaseInvoiceLineId,

            @NotNull(message = "Item ID is required")
            Long itemId,

            @NotNull(message = "Return quantity is required")
            @Positive(message = "Return quantity must be greater than zero")
            BigDecimal returnQuantity,

            BigDecimal unitPrice,

            PurchaseTaxCategory taxCategory,

            BigDecimal taxRate,

            String remarks
    ) {}
}

