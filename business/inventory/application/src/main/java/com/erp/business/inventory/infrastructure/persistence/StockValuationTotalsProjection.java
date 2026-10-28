package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;

/**
 * Projection for stock valuation totals query results.
 */
public interface StockValuationTotalsProjection {

    BigDecimal getTotalOnHandValue();

    BigDecimal getTotalReservedValue();

    BigDecimal getTotalAvailableValue();

    Long getItemsValued();

    Long getItemsMissingCost();
}
