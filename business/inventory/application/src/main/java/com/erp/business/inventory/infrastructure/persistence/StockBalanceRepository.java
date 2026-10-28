package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.StockBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link StockBalance} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByTenantIdAndCompanyIdAndWarehouseIdAndItemIdAndUnitId(
            Long tenantId, Long companyId, Long warehouseId, Long itemId, Long unitId);

    @Query(nativeQuery = true,
            value = """
                SELECT sb.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sb.unit_id AS unitId,
                       u.code AS unitCode,
                       sb.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sb.on_hand_qty AS onHand,
                       sb.reserved_qty AS reserved,
                       (sb.on_hand_qty - sb.reserved_qty) AS available,
                       sb.last_movement_at AS lastMovementAt
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND (:itemId IS NULL OR sb.item_id = :itemId)
                  AND (:inStockOnly = FALSE OR sb.on_hand_qty > 0)
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND (:itemId IS NULL OR sb.item_id = :itemId)
                  AND (:inStockOnly = FALSE OR sb.on_hand_qty > 0)
                """)
    Page<StockOnHandProjection> findOnHand(
            Long tenantId, Long companyId, Long warehouseId, Long itemId, boolean inStockOnly, Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                SELECT sb.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sb.unit_id AS unitId,
                       u.code AS unitCode,
                       sb.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sb.on_hand_qty AS onHand,
                       sb.reserved_qty AS reserved,
                       (sb.on_hand_qty - sb.reserved_qty) AS available,
                       i.min_stock_level AS minStockLevel,
                       i.reorder_level AS reorderLevel,
                       i.reorder_quantity AS reorderQuantity,
                       sb.last_movement_at AS lastMovementAt
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND (sb.on_hand_qty - sb.reserved_qty) <= COALESCE(i.reorder_level, 0)
                ORDER BY (sb.on_hand_qty - sb.reserved_qty) ASC NULLS FIRST
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND (sb.on_hand_qty - sb.reserved_qty) <= COALESCE(i.reorder_level, 0)
                """)
    Page<LowStockProjection> findLowStock(Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                SELECT sb.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sb.unit_id AS unitId,
                       u.code AS unitCode,
                       sb.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sb.on_hand_qty AS onHand,
                       sb.reserved_qty AS reserved,
                       (sb.on_hand_qty - sb.reserved_qty) AS available,
                       i.purchase_price AS unitCost,
                       (sb.on_hand_qty * i.purchase_price) AS onHandValue,
                       (sb.reserved_qty * i.purchase_price) AS reservedValue,
                       ((sb.on_hand_qty - sb.reserved_qty) * i.purchase_price) AS availableValue,
                       (i.purchase_price IS NULL) AS costBasisMissing,
                       sb.last_movement_at AS lastMovementAt
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty > 0
                ORDER BY (sb.on_hand_qty * i.purchase_price) DESC NULLS LAST
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty > 0
                """)
    Page<StockValuationProjection> findValuation(Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                SELECT SUM(sb.on_hand_qty * i.purchase_price) AS totalOnHandValue,
                       SUM(sb.reserved_qty * i.purchase_price) AS totalReservedValue,
                       SUM((sb.on_hand_qty - sb.reserved_qty) * i.purchase_price) AS totalAvailableValue,
                       COUNT(sb.item_id) AS itemsValued,
                       COUNT(*) FILTER (WHERE i.purchase_price IS NULL) AS itemsMissingCost
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty > 0
                """)
    StockValuationTotalsProjection sumValuation(Long tenantId, Long companyId, Long warehouseId);

    @Query(nativeQuery = true,
            value = """
                SELECT sb.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sb.unit_id AS unitId,
                       u.code AS unitCode,
                       sb.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sb.on_hand_qty AS onHand,
                       sb.reserved_qty AS reserved,
                       (sb.on_hand_qty - sb.reserved_qty) AS available,
                       w.allow_negative_stock AS negativeStockAllowed,
                       sb.last_movement_at AS lastMovementAt
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty < 0
                ORDER BY sb.on_hand_qty ASC NULLS LAST
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty < 0
                """)
    Page<NegativeStockProjection> findNegativeStock(Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                SELECT sb.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sb.unit_id AS unitId,
                       u.code AS unitCode,
                       sb.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sb.on_hand_qty AS onHand,
                       sb.reserved_qty AS reserved,
                       (sb.on_hand_qty - sb.reserved_qty) AS available,
                       i.purchase_price AS unitCost,
                       (sb.on_hand_qty * i.purchase_price) AS onHandValue,
                       sb.last_movement_at AS lastMovementAt,
                       EXTRACT(EPOCH FROM (now() - sb.last_movement_at)) / 86400 AS daysSinceMovement
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty > 0
                  AND sb.last_movement_at IS NOT NULL
                  AND EXTRACT(EPOCH FROM (now() - sb.last_movement_at)) / 86400 >= :minDays
                ORDER BY daysSinceMovement DESC NULLS LAST
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM stock_balances sb
                JOIN items i ON i.id = sb.item_id AND i.tenant_id = sb.tenant_id AND i.company_id = sb.company_id
                JOIN units u ON u.id = sb.unit_id AND u.tenant_id = sb.tenant_id AND u.company_id = sb.company_id
                JOIN warehouses w ON w.id = sb.warehouse_id AND w.tenant_id = sb.tenant_id AND w.company_id = sb.company_id
                WHERE sb.tenant_id = :tenantId
                  AND sb.company_id = :companyId
                  AND (:warehouseId IS NULL OR sb.warehouse_id = :warehouseId)
                  AND sb.on_hand_qty > 0
                  AND sb.last_movement_at IS NOT NULL
                  AND EXTRACT(EPOCH FROM (now() - sb.last_movement_at)) / 86400 >= :minDays
                """)
    Page<StockAgingProjection> findAging(Long tenantId, Long companyId, Long warehouseId, long minDays, Pageable pageable);
}
