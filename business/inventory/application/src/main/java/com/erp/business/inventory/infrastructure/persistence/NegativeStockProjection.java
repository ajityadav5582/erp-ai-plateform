package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for negative-stock query results.
 */
public interface NegativeStockProjection {

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

    boolean isNegativeStockAllowed();

    Instant getLastMovementAt();
}
