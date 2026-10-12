package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Warehouse;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Read access to warehouses.
 *
 * <p>Every finder is scoped by tenant <em>and</em> company. All companies inside a
 * tenant share one {@code tenantId}, so a tenant-only lookup would hand one company
 * another company's warehouses - and because a warehouse is the destination for
 * incoming stock, that is not a cosmetic leak: it would post goods into the wrong
 * company's books.
 */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /**
     * Loads a warehouse for a caller-supplied id, scoped to the company.
     *
     * <p>Ids arrive from request bodies, so company scoping is not optional here. A
     * lookup on {@code tenantId} alone would return another company's warehouse and
     * every subsequent write would land in the wrong tenant.
     */
    Optional<Warehouse> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    Optional<Warehouse> findByTenantIdAndCompanyIdAndCode(
            Long tenantId, Long companyId, String code);

    Optional<Warehouse> findByTenantIdAndCompanyIdAndName(
            Long tenantId, Long companyId, String name);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    /**
     * The company's single default warehouse, if it has one.
     *
     * <p>At most one row can match, and the database guarantees it - see the
     * {@code default_marker} discussion on {@link Warehouse}. That guarantee is what
     * makes {@link com.erp.business.inventory.warehouse.service.WarehouseServiceImpl}
     * safe: two callers racing to provision a default both end up pointing at the same
     * row instead of at two different ones.
     */
    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId "
         + "AND w.companyId = :companyId AND w.isDefault = true")
    Optional<Warehouse> findDefaultWarehouse(
            @Param("tenantId") Long tenantId, @Param("companyId") Long companyId);

    Page<Warehouse> findByTenantIdAndCompanyId(
            Long tenantId, Long companyId, Pageable pageable);

    List<Warehouse> findByTenantIdAndCompanyIdAndIsActiveTrueOrderByCodeAsc(
            Long tenantId, Long companyId);

    List<Warehouse> findByTenantIdAndCompanyIdAndBranchIdOrderByCodeAsc(
            Long tenantId, Long companyId, Long branchId);

    /**
     * Locks every warehouse in a company row set for the duration of the transaction.
     *
     * <p>Used by {@code setDefaultWarehouse}, which demotes one row and promotes
     * another. Without the lock two concurrent promotions interleave and the second
     * one trips the single-default constraint, failing the request with an error the
     * caller did nothing wrong to cause. Locking the whole company set - normally one
     * to a handful of rows - makes the swap atomic instead.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId "
         + "AND w.companyId = :companyId ORDER BY w.id")
    List<Warehouse> findAllForUpdate(
            @Param("tenantId") Long tenantId, @Param("companyId") Long companyId);
}
