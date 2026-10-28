package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Supplier} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Supplier> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    Page<Supplier> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Supplier> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);
}
