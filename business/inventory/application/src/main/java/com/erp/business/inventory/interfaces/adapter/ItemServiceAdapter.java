package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.items.dto.ItemResponse;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.platform.common.dto.PageRequest;
import com.erp.platform.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that implements the public {@link ItemService} interface from the
 * {@code business:inventory:interfaces} module by delegating to the internal
 * {@link com.erp.business.inventory.items.ItemService} implementation.
 *
 * @since 1.0.0
 */
@Component
public class ItemServiceAdapter implements ItemService {

    private final com.erp.business.inventory.items.ItemService internalItemService;
    private final BusinessContextAccessor contextAccessor;

    public ItemServiceAdapter(com.erp.business.inventory.items.ItemService internalItemService, BusinessContextAccessor contextAccessor) {
        this.internalItemService = internalItemService;
        this.contextAccessor = contextAccessor;
    }

    @Override
    public Optional<ItemService.ItemResponse> findById(Long itemId) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.items.dto.ItemResponse internal = internalItemService.getItemById(tenantId, companyId, itemId);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ItemService.ItemResponse> findBySku(String sku) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.items.dto.ItemResponse internal = internalItemService.getItemBySku(tenantId, companyId, sku);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ItemService.ItemResponse> findByBarcode(String barcode) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        try {
            com.erp.business.inventory.items.dto.ItemResponse internal = internalItemService.getItemByBarcode(tenantId, companyId, barcode);
            return Optional.of(mapToInterface(internal));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateItem(Long itemId) {
        return findById(itemId).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public boolean validatePurchasableItem(Long itemId) {
        return findById(itemId)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> Boolean.TRUE.equals(u.getIsPurchasable()))
                .orElse(false);
    }

    @Override
    public boolean validateSellableItem(Long itemId) {
        return findById(itemId)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> Boolean.TRUE.equals(u.getIsSellable()))
                .orElse(false);
    }

    @Override
    public boolean validateItemSku(String sku) {
        return findBySku(sku).map(u -> Boolean.TRUE.equals(u.getIsActive())).orElse(false);
    }

    @Override
    public List<ItemService.ItemResponse> findAllActive() {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        Page<com.erp.business.inventory.items.dto.ItemListResponse> page = internalItemService.listActiveItems(tenantId, companyId, Pageable.unpaged());
        return page.getContent().stream()
                .map(this::mapListToInterface)
                .toList();
    }

    @Override
    public PageResponse<ItemService.ItemResponse> findAll(PageRequest pageRequest) {
        Long tenantId = contextAccessor.getTenantId();
        Long companyId = contextAccessor.getCompanyId();
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size());
        Page<com.erp.business.inventory.items.dto.ItemListResponse> page = internalItemService.listItems(tenantId, companyId, pageable);
        List<ItemService.ItemResponse> content = page.getContent().stream()
                .map(this::mapListToInterface)
                .toList();
        PageResponse.Pagination pagination = PageResponse.Pagination.of(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        return PageResponse.of(content, pagination);
    }

    private ItemService.ItemResponse mapToInterface(com.erp.business.inventory.items.dto.ItemResponse internal) {
        return new ItemService.ItemResponse() {
            @Override public Long getId() { return internal.id(); }
            @Override public String getSku() { return internal.sku(); }
            @Override public String getBarcode() { return internal.barcode(); }
            @Override public String getName() { return internal.nameEn(); }
            @Override public String getDescription() { return internal.descriptionEn(); }
            @Override public Long getCategoryId() { return internal.categoryId(); }
            @Override public String getCategoryName() { return internal.categoryName(); }
            @Override public Long getUnitId() { return internal.uomId(); }
            @Override public String getUnitCode() { return internal.uomCode(); }
            @Override public String getUnitName() { return internal.uomName(); }
            @Override public String getItemType() { return internal.itemType().name(); }
            @Override public String getTaxabilityType() { return internal.taxabilityType().name(); }
            @Override public Boolean getIsActive() { return internal.isActive(); }
            @Override public Boolean getIsStockable() { return internal.isInventoryTracked(); }
            @Override public Long getAvailableStock() { return null; }
            @Override public Long getReservedStock() { return null; }
            @Override public java.math.BigDecimal getSellingPrice() { return internal.sellingPrice(); }
            @Override public java.math.BigDecimal getVatRate() { return internal.vatRate(); }
            @Override public Boolean getIsPurchasable() { return internal.isPurchasable(); }
            @Override public Boolean getIsSellable() { return internal.isSellable(); }
            @Override public Boolean getIsComponent() { return internal.isComponent(); }
            @Override public Boolean getIsManufactured() { return internal.isManufactured(); }
        };
    }

    private ItemService.ItemResponse mapListToInterface(com.erp.business.inventory.items.dto.ItemListResponse internal) {
        return new ItemService.ItemResponse() {
            @Override public Long getId() { return internal.id(); }
            @Override public String getSku() { return internal.sku(); }
            @Override public String getBarcode() { return internal.barcode(); }
            @Override public String getName() { return internal.nameEn(); }
            @Override public String getDescription() { return null; } // Not in list response
            @Override public Long getCategoryId() { return internal.categoryId(); }
            @Override public String getCategoryName() { return internal.categoryName(); }
            @Override public Long getUnitId() { return internal.uomId(); }
            @Override public String getUnitCode() { return internal.uomCode(); }
            @Override public String getUnitName() { return null; } // Not in list response
            @Override public String getItemType() { return internal.itemType().name(); }
            @Override public String getTaxabilityType() { return internal.taxabilityType().name(); }
            @Override public Boolean getIsActive() { return internal.isActive(); }
            @Override public Boolean getIsStockable() { return null; } // Not in list response
            @Override public Long getAvailableStock() { return null; }
            @Override public Long getReservedStock() { return null; }
            @Override public Boolean getIsPurchasable() { return internal.isPurchasable(); }
            @Override public Boolean getIsSellable() { return internal.isSellable(); }
            @Override public Boolean getIsComponent() { return internal.isComponent(); }
            @Override public Boolean getIsManufactured() { return internal.isManufactured(); }
        };
    }
}
