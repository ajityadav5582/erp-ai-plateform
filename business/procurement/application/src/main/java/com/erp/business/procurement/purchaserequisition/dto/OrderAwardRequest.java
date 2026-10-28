package com.erp.business.procurement.purchaserequisition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Records that approved demand has been awarded to a purchase order.
 *
 * <p>Awards are split per line so that one requisition can produce several orders,
 * and one line can be split across several suppliers.
 */
public record OrderAwardRequest(

        @NotNull(message = "Purchase order is required")
        Long purchaseOrderId,

        /**
         * {@code @Valid} is load-bearing, not decoration. Without it the
         * {@code @NotNull} on {@link LineAward#requisitionLineId()} is never
         * evaluated, a null line id reaches the service, and the award fails
         * somewhere downstream as a null dereference rather than as the 422
         * naming the offending field.
         */
        @NotEmpty(message = "At least one line award is required")
        @Valid
        List<LineAward> awards
) {
    public record LineAward(
            @NotNull(message = "Requisition line is required")
            Long requisitionLineId,

            @NotNull(message = "Awarded quantity is required")
            @Positive(message = "Awarded quantity must be greater than zero")
            BigDecimal orderedQuantity,

            Long purchaseOrderLineId
    ) {
    }
}
