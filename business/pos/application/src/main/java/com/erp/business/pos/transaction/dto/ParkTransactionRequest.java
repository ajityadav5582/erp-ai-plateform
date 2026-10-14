package com.erp.business.pos.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Park payload: persist a cart as an {@code OPEN} transaction so it can be
 * resumed later (another cashier, another terminal, after a reboot).
 *
 * <p>Parking takes no payment and allocates no document number — an abandoned
 * cart must never consume a number from the IRD-compliant sequential series.
 * Lines are re-priced server-side exactly as at checkout, so a resumed cart
 * always shows current prices.
 *
 * @param sessionId         the open cashier session the cart belongs to
 * @param customerId        customer id; {@code null} for a walk-in sale
 * @param customerName      customer name snapshot; defaults to "Walk-in Customer"
 * @param customerPanNumber PAN for B2B tax invoices; may be null for retail
 * @param lines             the cart; at least one line
 * @param notes             optional notes frozen onto the transaction
 */
public record ParkTransactionRequest(

        @NotNull(message = "Session is required")
        Long sessionId,

        Long customerId,

        String customerName,

        String customerPanNumber,

        @NotEmpty(message = "At least one item is required to park a cart")
        @Valid
        List<CheckoutLineRequest> lines,

        String notes
) {
}
