package com.erp.business.pos.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePosInvoiceRequest(
        @NotNull UUID idempotencyKey,
        @NotNull Long registerId,
        @NotNull Long sessionId,
        Long customerId,
        String customerPan,
        @NotNull String paymentMethod,
        BigDecimal tenderedAmount,
        BigDecimal changeAmount,
        @NotEmpty @Valid List<InvoiceLine> items
) {
    public record InvoiceLine(
            @NotNull Long itemId,
            String uomCode,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal unitPrice,
            BigDecimal discountAmount,
            boolean isTaxable
    ) {
    }
}
