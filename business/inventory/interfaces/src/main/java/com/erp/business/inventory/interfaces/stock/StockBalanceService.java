package com.erp.business.inventory.interfaces.stock;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application interface for Stock Balance queries.
 *
 * <p>Provides read-only access to stock levels for other modules (Sales, Procurement, etc.)
 * without exposing internal inventory implementation.
 *
 * @since 1.0.0
 */
public interface StockBalanceService {

    /**
     * Returns the stock balance for a specific item in a warehouse.
     */
    Optional<StockBalanceResponse> findByItemAndWarehouse(Long itemId, Long warehouseId);

    /**
     * Returns all stock balances for an item across all warehouses.
     */
    List<StockBalanceResponse> findByItem(Long itemId);

    /**
     * Returns all stock balances for a warehouse.
     */
    List<StockBalanceResponse> findByWarehouse(Long warehouseId);

    /**
     * Returns the on-hand quantity for an item in a warehouse.
     */
    BigDecimal getOnHand(Long itemId, Long warehouseId);

    /**
     * Returns the available quantity (on-hand minus reserved) for an item in a warehouse.
     */
    BigDecimal getAvailable(Long itemId, Long warehouseId);

    /**
     * Returns the reserved quantity for an item in a warehouse.
     */
    BigDecimal getReserved(Long itemId, Long warehouseId);

    /**
     * Returns all stock balances with pagination.
     */
    PageResponse<StockBalanceResponse> findAll(PageRequest pageRequest);

    interface StockBalanceResponse {
        Long getId();
        Long getItemId();
        String getItemCode();
        String getItemName();
        Long getWarehouseId();
        String getWarehouseCode();
        String getWarehouseName();
        Long getUnitId();
        String getUnitCode();
        BigDecimal getOnHandQuantity();
        BigDecimal getReservedQuantity();
        BigDecimal getAvailableQuantity();
        BigDecimal getMinStockLevel();
        BigDecimal getReorderLevel();
        BigDecimal getReorderQuantity();
    }
}
