package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.TaxabilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Tenant-scoped only. Must not be used to load an item addressed by an id from a
     * request: every company in the tenant shares one {@code tenantId}, so this
     * finder returns another company's item. Use
     * {@link #findByTenantIdAndCompanyIdAndId(Long, Long, Long)} for that.
     */
    Optional<Item> findByTenantIdAndId(Long tenantId, Long id);

    /**
     * Finds an item by tenant ID, company ID, and identifier.
     *
     * <p>This is the finder to use whenever the item id came from a path variable or
     * request body. Loading on tenant alone let one company read, modify, deactivate
     * or delete another company's item by guessing its id.
     *
     * @param tenantId  the tenant ID
     * @param companyId the company ID
     * @param id        the item identifier
     * @return the item if found within that tenant and company, empty otherwise
     */
    Optional<Item> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<Item> findByTenantIdAndCompanyIdAndSku(Long tenantId, Long companyId, String sku);

    Optional<Item> findByTenantIdAndCompanyIdAndBarcode(Long tenantId, Long companyId, String barcode);

    Page<Item> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    /**
     * All three listings below are paged rather than returning {@code List}.
     *
     * <p>These endpoints previously materialised every matching row into one JSON array.
     * On a company with a few thousand items that is a multi-megabyte response the client
     * cannot use and the server paid full price for: it loads the whole table into the
     * heap, then lazy-loads a category and a UOM per row while serialising (see
     * {@code ItemMapper}), which turns one query into two per item.
     */
    Page<Item> findByTenantIdAndCompanyIdAndIsActiveTrue(
            Long tenantId,
            Long companyId,
            Pageable pageable);

    Page<Item> findByTenantIdAndCompanyIdAndCategoryId(
            Long tenantId,
            Long companyId,
            Long categoryId,
            Pageable pageable);

    Page<Item> findByTenantIdAndCompanyIdAndTaxabilityType(
            Long tenantId,
            Long companyId,
            TaxabilityType taxabilityType,
            Pageable pageable);

    boolean existsByTenantIdAndCompanyIdAndSku(Long tenantId, Long companyId, String sku);

    boolean existsByTenantIdAndCompanyIdAndBarcode(Long tenantId, Long companyId, String barcode);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);

    /**
     * Substring search over the human-visible identifiers.
     *
     * <p>The {@code ESCAPE '\'} clause is load-bearing. Without it a caller searching for
     * {@code %} or {@code _} has their characters reinterpreted as LIKE wildcards, so
     * {@code %} returns the entire catalogue and {@code _} matches any single character.
     * The service escapes those characters before they reach here.
     *
     * <p>A leading-wildcard LIKE cannot use a B-tree index, so this stays a sequential
     * scan. That is acceptable at the current data size but is why the service also
     * rejects blank and over-long terms: a one-character search is the worst case, and an
     * unbounded term is the most expensive to evaluate.
     */
    @Query("SELECT i FROM Item i WHERE i.tenantId = :tenantId AND i.companyId = :companyId AND " +
           "(LOWER(i.nameEn) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(i.nameNp) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(i.sku) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\' OR " +
           "LOWER(i.hsCode) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\')")
    Page<Item> searchItems(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("query") String query,
            Pageable pageable);
}
