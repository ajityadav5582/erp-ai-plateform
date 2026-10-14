package com.erp.business.inventory.interfaces.item;

import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application interface for Item operations.
 *
 * <p>This interface defines the contract for item management that other modules
 * (e.g., Procurement, Sales) can depend on without coupling to Inventory's internal
 * implementation, JPA entities, or repositories.
 *
 * @since 1.0.0
 */
public interface ItemService {

    /**
     * Returns an item by its ID, scoped to the current tenant and company.
     *
     * @param itemId the item identifier
     * @return the item DTO if found and accessible
     */
    Optional<ItemResponse> findById(Long itemId);

    /**
     * Returns an item by its SKU, scoped to the current tenant and company.
     *
     * @param sku the item SKU
     * @return the item DTO if found and accessible
     */
    Optional<ItemResponse> findBySku(String sku);

    /**
     * Returns an item by its barcode, scoped to the current tenant and company.
     *
     * @param barcode the item barcode
     * @return the item DTO if found and accessible
     */
    Optional<ItemResponse> findByBarcode(String barcode);

    /**
     * Validates that an item exists, is active, and belongs to the current scope.
     *
     * @param itemId the item identifier to validate
     * @return true if valid and accessible
     */
    boolean validateItem(Long itemId);

    /**
     * Validates that an item SKU exists, is active, and belongs to the current scope.
     *
     * @param sku the item SKU to validate
     * @return true if valid and accessible
     */
    boolean validateItemSku(String sku);

    /**
     * Returns all active items for the current tenant and company.
     *
     * @return list of active item DTOs
     */
    List<ItemResponse> findAllActive();

    /**
     * Returns all items for the current tenant and company with pagination.
     *
     * @param pageRequest the pagination request
     * @return paginated response of item DTOs
     */
    PageResponse<ItemResponse> findAll(PageRequest pageRequest);

    /**
     * Data transfer object for Item information exposed to other modules.
     */
    interface ItemResponse {
        Long getId();
        String getSku();
        String getBarcode();
        String getName();
        String getDescription();
        Long getCategoryId();
        String getCategoryName();
        Long getUnitId();
        String getUnitCode();
        String getUnitName();
        String getItemType(); // PRODUCT, SERVICE, RAW_MATERIAL, etc.
        String getTaxabilityType(); // TAXABLE, EXEMPT, etc.
        Boolean getIsActive();
        Boolean getIsStockable();
        // Stock information (optional, for modules that need it)
        default Long getAvailableStock() { return null; }
        default Long getReservedStock() { return null; }

        // Pricing (optional). Defaulted to null so existing adapters that do not
        // carry price keep compiling; consumers such as POS read these to resolve
        // a selling price without coupling to Inventory internals.
        default BigDecimal getSellingPrice() { return null; }
        default BigDecimal getVatRate() { return null; }
    }

    /**
     * Request DTO for creating an item (used by Inventory's own interfaces).
     */
    interface ItemCreateRequest {
        String getSku();
        String getBarcode();
        String getName();
        String getDescription();
        Long getCategoryId();
        Long getUnitId();
        String getItemType();
        String getTaxabilityType();
        Boolean getIsActive();
        Boolean getIsStockable();
    }

    /**
     * Request DTO for updating an item (used by Inventory's own interfaces).
     */
    interface ItemUpdateRequest {
        String getBarcode();
        String getName();
        String getDescription();
        Long getCategoryId();
        Long getUnitId();
        String getItemType();
        String getTaxabilityType();
        Boolean getIsActive();
        Boolean getIsStockable();
    }
}
