package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;

/**
 * Projection for opening balance query results.
 */
public interface OpeningBalanceProjection {

    Long getItemId();

    Long getWarehouseId();

    Long getUnitId();

    BigDecimal getOnHand();
}
