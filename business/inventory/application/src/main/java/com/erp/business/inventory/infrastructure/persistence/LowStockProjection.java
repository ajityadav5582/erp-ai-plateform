package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for low-stock query results.
 */
public interface LowStockProjection {

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

    BigDecimal getMinStockLevel();

    BigDecimal getReorderLevel();

    BigDecimal getReorderQuantity();

    Instant getLastMovementAt();
}
