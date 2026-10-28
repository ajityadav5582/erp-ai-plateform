package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for stock valuation query results.
 */
public interface StockValuationProjection {

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

    BigDecimal getReservedValue();

    BigDecimal getAvailableValue();

    boolean isCostBasisMissing();

    Instant getLastMovementAt();
}
