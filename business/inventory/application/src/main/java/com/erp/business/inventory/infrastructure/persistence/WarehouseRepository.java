package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Warehouse} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    Optional<Warehouse> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Page<Warehouse> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Warehouse> findByTenantIdAndCompanyIdAndIsActiveTrueOrderByCodeAsc(Long tenantId, Long companyId);

    List<Warehouse> findByTenantIdAndCompanyIdAndBranchIdOrderByCodeAsc(Long tenantId, Long companyId, Long branchId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM warehouses WHERE tenant_id = :tenantId AND company_id = :companyId AND is_default = TRUE")
    Optional<Warehouse> findDefaultWarehouse(Long tenantId, Long companyId);

    @Query(nativeQuery = true,
            value = "SELECT * FROM warehouses WHERE tenant_id = :tenantId AND company_id = :companyId FOR UPDATE")
    List<Warehouse> findAllForUpdate(@Param("tenantId") Long tenantId,
                                     @Param("companyId") Long companyId);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);
}
