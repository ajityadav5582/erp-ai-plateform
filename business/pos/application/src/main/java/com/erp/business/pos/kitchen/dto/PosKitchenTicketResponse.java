package com.erp.business.pos.kitchen.dto;

public record PosKitchenTicketResponse(
        Long id,
        Long posTransactionId,
        String status
) {
}
