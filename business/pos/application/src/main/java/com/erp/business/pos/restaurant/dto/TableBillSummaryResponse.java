package com.erp.business.pos.restaurant.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TableBillSummaryResponse(
        Long tableId,
        String tableNumber,
        String section,
        Long sessionId,
        String waiter,
        Instant openedAt,
        int kotCount,
        List<BillLineItem> lines,
        BigDecimal subtotal,
        BigDecimal serviceChargePercent,
        BigDecimal serviceChargeAmount,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal totalAmount
) {
    public TableBillSummaryResponse(
            Long tableId,
            String tableNumber,
            String section,
            Long sessionId,
            String waiter,
            Instant openedAt,
            int kotCount,
            List<BillLineItem> lines,
            BigDecimal subtotal
    ) {
        this(
                tableId,
                tableNumber,
                section,
                sessionId,
                waiter,
                openedAt,
                kotCount,
                lines,
                subtotal,
                new BigDecimal("10.00"),
                subtotal != null ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO,
                new BigDecimal("13.00"),
                subtotal != null ? subtotal.multiply(new BigDecimal("1.10")).multiply(new BigDecimal("0.13")).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO,
                subtotal != null ? subtotal.multiply(new BigDecimal("1.10")).multiply(new BigDecimal("1.13")).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO
        );
    }
    public record BillLineItem(
            Long itemId,
            String itemCode,
            String itemName,
            String uomCode,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            String notes,
            String station,
            String modifiers,
            BigDecimal modifierPrice
    ) {
        public BillLineItem(Long itemId, String itemCode, String itemName, String uomCode,
                            BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineAmount, String notes) {
            this(itemId, itemCode, itemName, uomCode, quantity, unitPrice, lineAmount, notes, "KITCHEN", null, BigDecimal.ZERO);
        }
    }
}

