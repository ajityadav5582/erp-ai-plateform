package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.domain.TaxabilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Item} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByTenantIdAndCompanyIdAndSku(Long tenantId, Long companyId, String sku);

    Optional<Item> findByTenantIdAndCompanyIdAndBarcode(Long tenantId, Long companyId, String barcode);

    Optional<Item> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Page<Item> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<Item> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId, Pageable pageable);

    Page<Item> findByTenantIdAndCompanyIdAndCategoryId(Long tenantId, Long companyId, Long categoryId, Pageable pageable);

    Page<Item> findByTenantIdAndCompanyIdAndTaxabilityType(Long tenantId, Long companyId, TaxabilityType taxabilityType, Pageable pageable);

    boolean existsByTenantIdAndCompanyIdAndSku(Long tenantId, Long companyId, String sku);

    boolean existsByTenantIdAndCompanyIdAndBarcode(Long tenantId, Long companyId, String barcode);

    @Query("SELECT i FROM Item i WHERE i.tenantId = :tenantId AND i.companyId = :companyId " +
            "AND (LOWER(i.nameEn) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.barcode) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Item> searchItems(@Param("tenantId") Long tenantId,
                           @Param("companyId") Long companyId,
                           @Param("query") String query,
                           Pageable pageable);

    /**
     * Items eligible to appear on a Purchase Order or Requisition line.
     *
     * <p>Only active items with {@code isPurchasable = true} are returned so that
     * procurement item-pickers never show FINISHED_GOODS or SERVICE items.
     */
    Page<Item> findByTenantIdAndCompanyIdAndIsPurchasableTrueAndIsActiveTrue(
            Long tenantId, Long companyId, Pageable pageable);

    /**
     * Items eligible to appear on a Sales Invoice, Sales Order, or POS transaction line.
     *
     * <p>Only active items with {@code isSellable = true} are returned so that
     * POS / sales item-pickers never show RAW_MATERIAL or CONSUMABLE items.
     */
    Page<Item> findByTenantIdAndCompanyIdAndIsSellableTrueAndIsActiveTrue(
            Long tenantId, Long companyId, Pageable pageable);
}
