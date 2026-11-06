package com.erp.business.pos.session;

import java.math.BigDecimal;

public record SessionCloseResponse(
        BigDecimal closingCashCounted,
        BigDecimal expectedCash,
        BigDecimal cashVariance,
        BigDecimal grossSales,
        BigDecimal taxableSales,
        BigDecimal vatTotal,
        String startBillNumber,
        String endBillNumber,
        int totalBillCount,
        String zReportNumber
) {
}
