package com.erp.business.pos.pricing;

import com.erp.business.pos.domain.PosVatType;

import java.math.BigDecimal;

/**
 * A priced cart line for preview. The amounts are computed by the domain
 * ({@code PosTransactionLine}), not re-derived here, so a preview can never
 * disagree with what checkout will persist.
 */
public record PricedLine(
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercentage,
        PosVatType vatType,
        BigDecimal vatRate,
        BigDecimal lineAmount,
        BigDecimal vatAmount
) {
}
