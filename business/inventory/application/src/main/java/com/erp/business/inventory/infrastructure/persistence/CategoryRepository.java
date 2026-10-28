package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Category} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Category> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<Category> findByTenantIdAndCompanyIdAndSlug(Long tenantId, Long companyId, String slug);

    Optional<Category> findByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    Page<Category> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Category> findByTenantIdAndCompanyIdAndParentIsNull(Long tenantId, Long companyId);

    List<Category> findByTenantIdAndCompanyIdAndParentId(Long tenantId, Long companyId, Long parentId);

    List<Category> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    boolean existsByTenantIdAndCompanyIdAndSlug(Long tenantId, Long companyId, String slug);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.companyId = :companyId " +
            "ORDER BY c.id")
    List<Category> findHierarchyPathById(@Param("tenantId") Long tenantId,
                                         @Param("companyId") Long companyId,
                                         @Param("categoryId") Long categoryId);
}
