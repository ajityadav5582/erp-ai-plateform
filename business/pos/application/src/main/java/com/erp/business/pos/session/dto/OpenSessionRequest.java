package com.erp.business.pos.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to open a cashier session on a register.
 *
 * <p>The cashier is taken from the authenticated token, not the body. Only the
 * register and the opening cash count are supplied by the caller.
 */
public record OpenSessionRequest(

        @NotNull(message = "Register is required")
        Long registerId,

        @NotNull(message = "Opening cash amount is required")
        @PositiveOrZero(message = "Opening cash amount must not be negative")
        BigDecimal openingCashAmount,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {
}
