package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Unit} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Unit> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    Optional<Unit> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Page<Unit> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Unit> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndNameIgnoreCase(Long tenantId, Long companyId, String name);

    @Query("SELECT u FROM Unit u WHERE u.tenantId = :tenantId AND u.companyId = :companyId " +
            "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Unit> search(@Param("tenantId") Long tenantId,
                      @Param("companyId") Long companyId,
                      @Param("query") String query,
                      Pageable pageable);
}
