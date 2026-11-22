package com.erp.business.pos.restaurant.dto;

import com.erp.business.pos.restaurant.domain.KitchenOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record KitchenTicketResponse(
        Long orderId,
        String kotNumber,
        Long tableId,
        String tableNumber,
        String section,
        String waiter,
        String orderType,
        String tokenNumber,
        String deliveryPartner,
        String customerPhone,
        Instant firedAt,
        KitchenOrder.Status status,
        int course,
        String notes,
        List<TicketLine> lines
) {
    public KitchenTicketResponse(
            Long orderId,
            String kotNumber,
            Long tableId,
            String tableNumber,
            String section,
            String waiter,
            String orderType,
            Instant firedAt,
            KitchenOrder.Status status,
            int course,
            String notes,
            List<TicketLine> lines
    ) {
        this(orderId, kotNumber, tableId, tableNumber, section, waiter, orderType, null, null, null, firedAt, status, course, notes, lines);
    }

    public record TicketLine(
            Long itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            String station,
            String modifiers,
            BigDecimal modifierPrice,
            String notes,
            int course
    ) {}
}

