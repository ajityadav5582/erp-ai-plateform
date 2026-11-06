package com.erp.business.pos.reports.salesbook;

import java.math.BigDecimal;

public record SalesBookRow(
        String dateBs,
        String dateAd,
        String billNumber,
        String buyerName,
        String buyerPan,
        BigDecimal totalSales,
        BigDecimal exemptSales,
        BigDecimal zeroRatedSales,
        BigDecimal taxableSales,
        BigDecimal vatAmount,
        String status
) {
}
