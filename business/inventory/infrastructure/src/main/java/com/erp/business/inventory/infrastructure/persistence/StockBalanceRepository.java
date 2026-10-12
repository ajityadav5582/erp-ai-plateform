package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.StockBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Page<StockBalance> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<StockBalance> findByTenantIdAndCompanyIdAndWarehouseId(
            Long tenantId, Long companyId, Long warehouseId, Pageable pageable);

    Page<StockBalance> findByTenantIdAndCompanyIdAndItemId(
            Long tenantId, Long companyId, Long itemId, Pageable pageable);

    Optional<StockBalance> findByTenantIdAndCompanyIdAndWarehouseIdAndItemIdAndUnitId(
            Long tenantId, Long companyId, Long warehouseId, Long itemId, Long unitId);

    List<StockBalance> findByTenantIdAndCompanyIdAndItemIdAndWarehouseIdIn(
            Long tenantId, Long companyId, Long itemId, List<Long> warehouseIds);

    @Query("SELECT b FROM StockBalance b WHERE b.tenantId = :tenantId AND b.companyId = :companyId " +
           "AND b.onHandQty > 0 ORDER BY b.onHandQty ASC")
    Page<StockBalance> findWithStock(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.onHandQty), 0) FROM StockBalance b " +
           "WHERE b.tenantId = :tenantId AND b.companyId = :companyId " +
           "AND b.itemId = :itemId AND b.unitId = :unitId")
    BigDecimal totalOnHand(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("itemId") Long itemId,
            @Param("unitId") Long unitId);

    @Query("SELECT COALESCE(SUM(b.onHandQty - b.reservedQty), 0) FROM StockBalance b " +
           "WHERE b.tenantId = :tenantId AND b.companyId = :companyId " +
           "AND b.itemId = :itemId AND b.unitId = :unitId")
    BigDecimal totalAvailable(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("itemId") Long itemId,
            @Param("unitId") Long unitId);
}
