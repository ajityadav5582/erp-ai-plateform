package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for stock period summary query results.
 */
public interface StockPeriodSummaryProjection {

    Long getItemId();

    String getSku();

    String getItemName();

    Long getWarehouseId();

    String getWarehouseCode();

    String getWarehouseName();

    Long getUnitId();

    String getUnitCode();

    Instant getPeriodStart();

    Instant getPeriodEnd();

    BigDecimal getOpeningQuantity();

    BigDecimal getTotalIn();

    BigDecimal getTotalOut();

    BigDecimal getTotalAdjust();

    BigDecimal getClosingQuantity();
}
