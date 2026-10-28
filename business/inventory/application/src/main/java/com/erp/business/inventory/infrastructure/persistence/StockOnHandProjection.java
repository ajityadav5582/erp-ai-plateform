package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for stock-on-hand query results.
 */
public interface StockOnHandProjection {

    Long getItemId();

    String getSku();

    String getItemName();

    Long getUnitId();

    String getUnitCode();

    Long getWarehouseId();

    String getWarehouseCode();

    String getWarehouseName();

    BigDecimal getOnHand();

    BigDecimal getReserved();

    BigDecimal getAvailable();

    Instant getLastMovementAt();
}
