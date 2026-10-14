package com.erp.business.pos.session.dto;

import com.erp.business.pos.domain.PosSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model for a POS cashier session, including the running shift totals and
 * the cash variance computed at close.
 */
public record PosSessionResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String sessionNumber,
        Long registerId,
        String registerCode,
        String registerName,
        Long fiscalYearId,
        String cashierId,
        String cashierName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingCashAmount,
        BigDecimal closingCashAmount,
        BigDecimal expectedCash,
        BigDecimal cashVariance,
        int totalTransactions,
        int totalVoided,
        BigDecimal totalCashSales,
        BigDecimal totalCardSales,
        BigDecimal totalWalletSales,
        BigDecimal totalOtherSales,
        BigDecimal totalVatCollected,
        BigDecimal grossSales,
        PosSessionStatus status,
        String notes,
        String closeNotes,
        Integer version
) {
}
