package com.erp.business.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection for stock ledger query results.
 */
public interface StockLedgerProjection {

    Instant getMovementDate();

    String getMovementType();

    String getReferenceType();

    String getReferenceNumber();

    String getSku();

    String getItemName();

    BigDecimal getQuantity();

    BigDecimal getRunningBalance();

    String getPostedBy();

    String getNotes();

    String getWarehouseCode();

    String getWarehouseName();

    String getUnitCode();
}
