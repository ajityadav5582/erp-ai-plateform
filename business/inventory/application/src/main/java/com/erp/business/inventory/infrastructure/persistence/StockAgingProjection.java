package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for stock aging query results.
 */
public interface StockAgingProjection {

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

    BigDecimal getUnitCost();

    BigDecimal getOnHandValue();

    Instant getLastMovementAt();

    Long getDaysSinceMovement();
}
