package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Persistence port for the {@link StockMovement} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByTenantIdAndCompanyIdAndReferenceTypeAndReferenceIdAndMovementType(
            Long tenantId, Long companyId, String referenceType, Long referenceId, StockMovementType movementType);

    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    Page<StockMovement> findByTenantIdAndCompanyIdAndWarehouseId(
            Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndCompanyIdAndItemId(
            Long tenantId, Long companyId, Long itemId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndCompanyIdAndWarehouseIdAndItemId(
            Long tenantId, Long companyId, Long warehouseId, Long itemId, Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                SELECT COALESCE(SUM(
                    CASE sm.movement_type
                        WHEN 'IN' THEN sm.quantity
                        WHEN 'ADJUST' THEN sm.quantity
                        WHEN 'RELEASE' THEN sm.quantity
                        WHEN 'OUT' THEN -sm.quantity
                        WHEN 'RESERVE' THEN -sm.quantity
                        ELSE 0
                    END
                ), 0) AS sumOnHandBefore
                FROM stock_movements sm
                WHERE sm.tenant_id = :tenantId
                  AND sm.company_id = :companyId
                  AND (:itemId IS NULL OR sm.item_id = :itemId)
                  AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                  AND sm.movement_date < :before
                """)
    BigDecimal sumOnHandBefore(Long tenantId, Long companyId, Long itemId, Long warehouseId, Instant before);

    @Query(nativeQuery = true,
            value = """
                SELECT sm.movement_date AS movementDate,
                       sm.movement_type AS movementType,
                       sm.reference_type AS referenceType,
                       sm.reference_number AS referenceNumber,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sm.quantity AS quantity,
                       sm.posted_by AS postedBy,
                       sm.notes AS notes,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       u.code AS unitCode
                FROM stock_movements sm
                JOIN items i ON i.id = sm.item_id AND i.tenant_id = sm.tenant_id AND i.company_id = sm.company_id
                JOIN warehouses w ON w.id = sm.warehouse_id AND w.tenant_id = sm.tenant_id AND w.company_id = sm.company_id
                JOIN units u ON u.id = sm.unit_id AND u.tenant_id = sm.tenant_id AND u.company_id = sm.company_id
                WHERE sm.tenant_id = :tenantId
                  AND sm.company_id = :companyId
                  AND (:itemId IS NULL OR sm.item_id = :itemId)
                  AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                  AND (:type IS NULL OR sm.movement_type = :type)
                  AND sm.movement_date >= :from
                  AND sm.movement_date <= :to
                ORDER BY sm.movement_date ASC, sm.id ASC
                """)
    List<StockLedgerProjection> findLedger(
            Long tenantId, Long companyId, Long itemId, Long warehouseId, String type, Instant from, Instant to);

    @Query(nativeQuery = true,
            value = """
                SELECT sm.item_id AS itemId,
                       i.sku AS sku,
                       i.name_en AS itemName,
                       sm.warehouse_id AS warehouseId,
                       w.code AS warehouseCode,
                       w.name AS warehouseName,
                       sm.unit_id AS unitId,
                       u.code AS unitCode,
                       :from AS periodStart,
                       :to AS periodEnd,
                       COALESCE((
                           SELECT COALESCE(SUM(
                               CASE sm2.movement_type
                                   WHEN 'IN' THEN sm2.quantity
                                   WHEN 'ADJUST' THEN sm2.quantity
                                   WHEN 'RELEASE' THEN sm2.quantity
                                   WHEN 'OUT' THEN -sm2.quantity
                                   WHEN 'RESERVE' THEN -sm2.quantity
                                   ELSE 0
                               END
                           ), 0)
                           FROM stock_movements sm2
                           WHERE sm2.tenant_id = sm.tenant_id
                             AND sm2.company_id = sm.company_id
                             AND sm2.item_id = sm.item_id
                             AND sm2.warehouse_id = sm.warehouse_id
                             AND sm2.movement_date < :from
                       ), 0) AS openingQuantity,
                       COALESCE(SUM(
                           CASE sm.movement_type
                               WHEN 'IN' THEN sm.quantity
                               WHEN 'RELEASE' THEN sm.quantity
                               ELSE 0
                           END
                       ), 0) AS totalIn,
                       COALESCE(SUM(
                           CASE sm.movement_type
                               WHEN 'OUT' THEN sm.quantity
                               WHEN 'RESERVE' THEN sm.quantity
                               ELSE 0
                           END
                       ), 0) AS totalOut,
                       COALESCE(SUM(
                           CASE sm.movement_type
                               WHEN 'ADJUST' THEN sm.quantity
                               ELSE 0
                           END
                       ), 0) AS totalAdjust,
                       COALESCE((
                           SELECT COALESCE(SUM(
                               CASE sm2.movement_type
                                   WHEN 'IN' THEN sm2.quantity
                                   WHEN 'ADJUST' THEN sm2.quantity
                                   WHEN 'RELEASE' THEN sm2.quantity
                                   WHEN 'OUT' THEN -sm2.quantity
                                   WHEN 'RESERVE' THEN -sm2.quantity
                                   ELSE 0
                               END
                           ), 0)
                           FROM stock_movements sm2
                           WHERE sm2.tenant_id = sm.tenant_id
                             AND sm2.company_id = sm.company_id
                             AND sm2.item_id = sm.item_id
                             AND sm2.warehouse_id = sm.warehouse_id
                             AND sm2.movement_date <= :to
                       ), 0) AS closingQuantity
                FROM stock_movements sm
                JOIN items i ON i.id = sm.item_id AND i.tenant_id = sm.tenant_id AND i.company_id = sm.company_id
                JOIN warehouses w ON w.id = sm.warehouse_id AND w.tenant_id = sm.tenant_id AND w.company_id = sm.company_id
                JOIN units u ON u.id = sm.unit_id AND u.tenant_id = sm.tenant_id AND u.company_id = sm.company_id
                WHERE sm.tenant_id = :tenantId
                  AND sm.company_id = :companyId
                  AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                  AND sm.movement_date >= :from
                  AND sm.movement_date <= :to
                GROUP BY sm.item_id, i.sku, i.name_en, sm.warehouse_id, w.code, w.name, sm.unit_id, u.code
                ORDER BY i.sku, w.code, u.code
                """)
    List<StockPeriodSummaryProjection> summarizePeriod(
            Long tenantId, Long companyId, Long warehouseId, Instant from, Instant to);

    @Query(nativeQuery = true,
            value = """
                SELECT sm.item_id AS itemId,
                       sm.warehouse_id AS warehouseId,
                       sm.unit_id AS unitId,
                       COALESCE(SUM(
                           CASE sm.movement_type
                               WHEN 'IN' THEN sm.quantity
                               WHEN 'ADJUST' THEN sm.quantity
                               WHEN 'RELEASE' THEN sm.quantity
                               WHEN 'OUT' THEN -sm.quantity
                               WHEN 'RESERVE' THEN -sm.quantity
                               ELSE 0
                           END
                       ), 0) AS onHand
                FROM stock_movements sm
                WHERE sm.tenant_id = :tenantId
                  AND sm.company_id = :companyId
                  AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
                  AND sm.movement_date < :before
                GROUP BY sm.item_id, sm.warehouse_id, sm.unit_id
                """)
    List<OpeningBalanceProjection> sumOnHandBeforeGrouped(
            Long tenantId, Long companyId, Long warehouseId, Instant before);
}
