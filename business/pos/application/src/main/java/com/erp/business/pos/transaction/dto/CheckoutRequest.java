package com.erp.business.pos.transaction.dto;

import com.erp.business.pos.domain.PosPaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout payload: one atomic call that finalises a sale.
 *
 * <p><strong>Server-authoritative pricing.</strong> The caller supplies only the
 * item, the quantity and an optional line discount. The unit price and VAT are
 * re-resolved from the Inventory catalogue at checkout time, so a price that
 * drifted between add-to-cart and pay can never be honoured, and a tampered
 * client cannot write its own price.
 *
 * <p>Single tender only in v1: one {@code paymentMethod} per transaction. Split
 * tender needs product sign-off and a {@code payments[]} shape.
 *
 * @param sessionId         the open cashier session this sale belongs to
 * @param customerId        customer id; {@code null} for a walk-in sale
 * @param customerName      customer name snapshot; defaults to "Walk-in Customer"
 * @param customerPanNumber PAN for B2B tax invoices; may be null for retail
 * @param lines             the cart; at least one line
 * @param paymentMethod     how the customer is paying
 * @param amountTendered    for CASH: the amount handed over; defaults to the total
 * @param paymentReference  card approval code / wallet transaction id; may be null
 * @param notes             optional notes frozen onto the transaction
 */
public record CheckoutRequest(

        @NotNull(message = "Session is required")
        Long sessionId,

        Long customerId,

        String customerName,

        String customerPanNumber,

        @NotEmpty(message = "At least one item is required to check out")
        @Valid
        List<CheckoutLineRequest> lines,

        @NotNull(message = "Payment method is required")
        PosPaymentMethod paymentMethod,

        BigDecimal amountTendered,

        String paymentReference,

        String notes
) {
}
