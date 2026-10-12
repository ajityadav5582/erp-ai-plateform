package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.StockMovement;
import com.erp.business.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndCompanyIdAndWarehouseId(
            Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndCompanyIdAndItemId(
            Long tenantId, Long companyId, Long itemId, Pageable pageable);

    Page<StockMovement> findByTenantIdAndCompanyIdAndWarehouseIdAndItemId(
            Long tenantId, Long companyId, Long warehouseId, Long itemId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * The movements already posted for one source document in one direction.
     *
     * <p>This is the idempotency check for a batch: a receipt (IN) and its
     * reversal (OUT) share a reference, so the direction is what tells an
     * already-posted receipt from an already-posted reversal.
     */
    List<StockMovement> findByReferenceTypeAndReferenceIdAndMovementType(
            String referenceType, Long referenceId, StockMovementType movementType);

    @Query("SELECT m FROM StockMovement m WHERE m.tenantId = :tenantId AND m.companyId = :companyId " +
           "AND m.warehouseId = :warehouseId AND m.itemId = :itemId AND m.unitId = :unitId " +
           "AND m.movementDate BETWEEN :from AND :to ORDER BY m.movementDate DESC")
    Page<StockMovement> findByScopeAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("itemId") Long itemId,
            @Param("unitId") Long unitId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.tenantId = :tenantId AND m.companyId = :companyId " +
           "AND m.warehouseId = :warehouseId AND m.itemId = :itemId AND m.unitId = :unitId " +
           "AND m.movementType IN ('IN', 'ADJUST')")
    java.math.BigDecimal sumInbound(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("itemId") Long itemId,
            @Param("unitId") Long unitId);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.tenantId = :tenantId AND m.companyId = :companyId " +
           "AND m.warehouseId = :warehouseId AND m.itemId = :itemId AND m.unitId = :unitId " +
           "AND m.movementType IN ('OUT', 'RESERVE')")
    java.math.BigDecimal sumOutbound(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("warehouseId") Long warehouseId,
            @Param("itemId") Long itemId,
            @Param("unitId") Long unitId);
}
