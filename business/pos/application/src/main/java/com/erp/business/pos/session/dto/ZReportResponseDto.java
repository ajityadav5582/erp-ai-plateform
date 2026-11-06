package com.erp.business.pos.session.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ZReportResponseDto(
        String companyName,
        String panNumber,
        String counterName,
        String cashierName,
        Instant shiftStart,
        Instant shiftEnd,
        String startBillNumber,
        String endBillNumber,
        int billCount,
        BigDecimal grossSales,
        BigDecimal totalTaxable,
        BigDecimal totalVat,
        BigDecimal totalExempt,
        BigDecimal cashSales,
        BigDecimal qrSales,
        BigDecimal cardSales,
        BigDecimal creditSales,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal cashVariance
) {
}
