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
 * Spring Data JPA repository for Category aggregate.
 *
 * <p>Provides data access methods for category management operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by tenant ID and its identifier.
     *
     * <p>Tenant-scoped only. Safe for reads that are already inside a company
     * boundary, but must not be used to resolve a category named by a client:
     * every company in the tenant shares one {@code tenantId}, so this finder will
     * happily return another company's category. Use
     * {@link #findByTenantIdAndCompanyIdAndId(Long, Long, Long)} wherever the
     * category id originates in a request body.
     *
     * @param tenantId the tenant ID
     * @param entityId the category identifier
     * @return the category if found, empty otherwise
     */
    Optional<Category> findByTenantIdAndId(Long tenantId, Long entityId);

    /**
     * Finds a category by tenant ID, company ID, and identifier.
     *
     * <p>This is the finder to use when the category id came from a request body.
     * Resolving it against tenant alone let one company attach another company's
     * category to its records.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param entityId  the category identifier
     * @return the category if found within that tenant and company, empty otherwise
     */
    Optional<Category> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long entityId);

    /**
     * Finds a category by tenant ID, company ID, and slug.
     *
     * <p>Used for lookups during category creation and validation.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param slug      the category slug
     * @return the category if found, empty otherwise
     */
    Optional<Category> findByTenantIdAndCompanyIdAndSlug(Long tenantId, Long companyId, String slug);

    /**
     * Finds a category by tenant ID, company ID, and name.
     *
     * <p>Used for lookups during category creation and validation.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param name      the category name
     * @return the category if found, empty otherwise
     */
    Optional<Category> findByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    /**
     * Finds all categories for a specific tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of categories for the tenant and company
     */
    List<Category> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    /**
     * Finds all categories for a specific tenant and company, paginated.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param pageable  the pagination parameters
     * @return a page of categories for the tenant and company
     */
    Page<Category> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Finds all root categories (categories without parent) for a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of root categories
     */
    List<Category> findByTenantIdAndCompanyIdAndParentIsNull(Long tenantId, Long companyId);

    /**
     * Finds all root categories for a tenant and company, paginated.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param pageable  the pagination parameters
     * @return a page of root categories
     */
    Page<Category> findByTenantIdAndCompanyIdAndParentIsNull(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Finds all child categories of a parent category.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param parentId  the parent category ID
     * @return list of child categories
     */
    List<Category> findByTenantIdAndCompanyIdAndParentId(Long tenantId, Long companyId, Long parentId);

    /**
     * Finds all active categories for a specific tenant and company.
     *
     * <p>Used for operational queries and dropdown selections.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of active categories for the tenant and company
     */
    List<Category> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    /**
     * Finds all active root categories for a specific tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return list of active root categories
     */
    List<Category> findByTenantIdAndCompanyIdAndParentIsNullAndIsActiveTrue(Long tenantId, Long companyId);

    /**
     * Checks if a category slug exists within a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param slug      the category slug to check
     * @return true if the category slug exists, false otherwise
     */
    boolean existsByTenantIdAndCompanyIdAndSlug(Long tenantId, Long companyId, String slug);

    /**
     * Checks if a category name exists within a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param name      the category name to check
     * @return true if the category name exists, false otherwise
     */
    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    /**
     * Checks if a category exists by ID, tenant ID, and company ID.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param id        the category ID
     * @return true if the category exists, false otherwise
     */
    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    /**
     * Counts the number of categories for a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return the count of categories
     */
    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);

    /**
     * Counts the number of active categories for a tenant and company.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @return the count of active categories
     */
    long countByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    /**
     * Finds categories by tenant, company, and name containing a search term (case-insensitive).
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param name      the search term
     * @param pageable  the pagination parameters
     * @return a page of matching categories
     */
    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.companyId = :companyId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Category> findByTenantIdAndCompanyIdAndNameContainingIgnoreCase(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("name") String name,
            Pageable pageable);

    /**
     * Finds all categories in a hierarchy path (from root to the specified category).
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param categoryId the target category ID
     * @return list of categories from root to the target category
     */
    @Query(
            value = """
                    WITH RECURSIVE category_path AS (
                        SELECT * FROM categories WHERE id = :categoryId AND tenant_id = :tenantId AND company_id = :companyId
                        UNION ALL
                        SELECT c.* FROM categories c
                        INNER JOIN category_path cp ON c.id = cp.parent_id
                        WHERE c.tenant_id = :tenantId AND c.company_id = :companyId
                    )
                    SELECT * FROM category_path ORDER BY id
                    """,
            nativeQuery = true)
    List<Category> findHierarchyPathById(@Param("tenantId") Long tenantId,
                                          @Param("companyId") Long companyId,
                                          @Param("categoryId") Long categoryId);
}
