package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Unit;
import com.erp.business.inventory.domain.UnitDimension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Unit}.
 *
 * <p>Every finder is prefixed with {@code TenantId} and, where company scoping
 * applies, {@code CompanyId}. That is not redundancy: it is the guarantee that a
 * request can only ever reach rows belonging to the tenant on its token and the
 * company in its {@code X-Company-Id} header. A bare {@code findById} would
 * leak across tenants the moment an id was guessed.
 */
public interface UnitRepository extends JpaRepository<Unit, Long> {

    /**
     * Tenant-scoped only. Must not be used to resolve a unit named by a client: every
     * company in the tenant shares one {@code tenantId}, so this finder will return
     * another company's unit. Use
     * {@link #findByTenantIdAndCompanyIdAndId(Long, Long, Long)} for that.
     */
    Optional<Unit> findByTenantIdAndId(Long tenantId, Long id);

    /**
     * Finds a unit by tenant ID, company ID, and identifier.
     *
     * <p>This is the finder to use when the unit id came from a request body. Resolving
     * it against tenant alone let one company assign another company's unit to its
     * items.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param id        the unit identifier
     * @return the unit if found within that tenant and company, empty otherwise
     */
    Optional<Unit> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<Unit> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    Optional<Unit> findByTenantIdAndCompanyIdAndNameIgnoreCase(Long tenantId, Long companyId, String name);

    List<Unit> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    Page<Unit> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Unit> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    List<Unit> findByTenantIdAndCompanyIdAndDimension(Long tenantId, Long companyId, UnitDimension dimension);

    List<Unit> findByTenantIdAndCompanyIdAndDimensionAndIsActiveTrue(
            Long tenantId, Long companyId, UnitDimension dimension);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndNameIgnoreCase(Long tenantId, Long companyId, String name);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);

    long countByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    /**
     * Free-text search across both the display name and the short code.
     *
     * <p>Matching the code as well as the name is specific to units: users type
     * "kg" far more often than "kilogram", so a name-only search (which is all the
     * supplier list offers) would return nothing for the most common query.
     */
    @Query("""
            SELECT u FROM Unit u
            WHERE u.tenantId = :tenantId
              AND u.companyId = :companyId
              AND (
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
                 OR LOWER(u.code) LIKE LOWER(CONCAT('%', :term, '%'))
              )
            """)
    Page<Unit> search(@Param("tenantId") Long tenantId,
                      @Param("companyId") Long companyId,
                      @Param("term") String term,
                      Pageable pageable);
}
