package com.erp.business.inventory.items.mapper;

import com.erp.business.inventory.domain.Item;
import com.erp.business.inventory.items.dto.ItemListResponse;
import com.erp.business.inventory.items.dto.ItemResponse;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public ItemResponse toResponse(Item item) {
        if (item == null) {
            return null;
        }

        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;

        Long uomId = item.getUom() != null ? item.getUom().getId() : null;
        String uomCode = item.getUom() != null ? item.getUom().getCode() : null;
        String uomName = item.getUom() != null ? item.getUom().getName() : null;

        return new ItemResponse(
                item.getId(),
                item.getTenantId(),
                item.getCompanyId(),
                item.getSku(),
                item.getBarcode(),
                item.getHsCode(),
                item.getNameEn(),
                item.getNameNp(),
                item.getDescriptionEn(),
                item.getDescriptionNp(),
                categoryId,
                categoryName,
                uomId,
                uomCode,
                uomName,
                item.getItemType(),
                item.getTaxabilityType(),
                item.getVatRate(),
                item.getExciseType(),
                item.getExciseRate(),
                item.getExciseAmount(),
                item.getSellingPrice(),
                item.getPurchasePrice(),
                item.getMrp(),
                item.getIsVatInclusive(),
                item.getIsInventoryTracked(),
                item.getIsBatchTracked(),
                item.getIsExpiryTracked(),
                item.getMinStockLevel(),
                item.getReorderLevel(),
                item.getReorderQuantity(),
                item.getIsActive(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getCreatedBy(),
                item.getUpdatedBy(),
                item.getVersion()
        );
    }

    public ItemListResponse toListResponse(Item item) {
        if (item == null) {
            return null;
        }

        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;

        Long uomId = item.getUom() != null ? item.getUom().getId() : null;
        String uomCode = item.getUom() != null ? item.getUom().getCode() : null;

        return new ItemListResponse(
                item.getId(),
                item.getSku(),
                item.getBarcode(),
                item.getHsCode(),
                item.getNameEn(),
                item.getNameNp(),
                categoryId,
                categoryName,
                uomId,
                uomCode,
                item.getItemType(),
                item.getTaxabilityType(),
                item.getVatRate(),
                item.getSellingPrice(),
                item.getPurchasePrice(),
                item.getMrp(),
                item.getIsActive()
        );
    }
}
