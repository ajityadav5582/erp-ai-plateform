package com.erp.business.procurement.purchaseorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Converts an approved requisition into a new draft purchase order.
 *
 * <p><strong>Only the supplier is asked for.</strong> Everything else on the order -
 * item, UOM, quantity, planning price, tax rate, currency, branch, fiscal year and
 * delivery location - is copied from the requisition. Re-entering any of it would let
 * the order disagree with the approval that authorised it.
 *
 * <p><strong>The supplier cannot be copied</strong> because a requisition names no
 * supplier: a branch asks for "500 chairs" long before anyone knows who will supply
 * them. So the buyer names the supplier at conversion, and the code/name pair is a
 * frozen snapshot taken here rather than read back from the catalogue later.
 */
public record ConvertToPurchaseOrderRequest(

        @NotNull(message = "Purchase requisition is required")
        @Positive(message = "Purchase requisition id must be positive")
        Long requisitionId,

        @NotNull(message = "Supplier is required")
        @Positive(message = "Supplier id must be positive")
        Long supplierId,

        @NotBlank(message = "Supplier code is required")
        @Size(max = 64)
        String supplierCode,

        @NotBlank(message = "Supplier name is required")
        @Size(max = 255)
        String supplierName,

        /** Delivery commitment. Falls back to the requisition's needed-by date. */
        LocalDate expectedDeliveryDate,

        @Size(max = 2000)
        String notes
) {
}
