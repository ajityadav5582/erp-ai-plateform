package com.erp.business.pos.pricing;

import com.erp.business.pos.domain.PosVatType;

import java.math.BigDecimal;

/**
 * The resolved selling price and VAT treatment for one item, derived from the
 * Inventory catalogue. Immutable; produced by {@link PosPricingService}.
 */
public record ResolvedPrice(
        BigDecimal unitPrice,
        PosVatType vatType,
        BigDecimal vatRate
) {
}
