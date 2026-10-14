package com.erp.business.pos.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to close a cashier session, carrying the physical cash counted in the
 * drawer so the service can compute the cash variance.
 */
public record CloseSessionRequest(

        @NotNull(message = "Closing cash amount is required")
        @PositiveOrZero(message = "Closing cash amount must not be negative")
        BigDecimal closingCashAmount,

        @Size(max = 2000, message = "Close notes must not exceed 2000 characters")
        String closeNotes
) {
}
