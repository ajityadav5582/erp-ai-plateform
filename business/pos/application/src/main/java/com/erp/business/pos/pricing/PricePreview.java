package com.erp.business.pos.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Header totals for a cart preview. {@code subtotal} is the sum of net line
 * amounts (before VAT), {@code vatTotal} the sum of line VAT, and {@code total}
 * their sum — the same roll-up the transaction aggregate performs at checkout.
 */
public record PricePreview(
        List<PricedLine> lines,
        BigDecimal subtotal,
        BigDecimal vatTotal,
        BigDecimal total
) {
}
