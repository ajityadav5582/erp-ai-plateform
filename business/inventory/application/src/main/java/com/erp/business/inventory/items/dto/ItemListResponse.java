package com.erp.business.inventory.items.dto;

import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;

import java.math.BigDecimal;

public record ItemListResponse(
        Long id,
        String sku,
        String barcode,
        String hsCode,
        String nameEn,
        String nameNp,
        Long categoryId,
        String categoryName,
        Long uomId,
        String uomCode,
        ItemType itemType,
        TaxabilityType taxabilityType,
        BigDecimal vatRate,
        BigDecimal sellingPrice,
        BigDecimal purchasePrice,
        BigDecimal mrp,
        Boolean isActive
) {
}
