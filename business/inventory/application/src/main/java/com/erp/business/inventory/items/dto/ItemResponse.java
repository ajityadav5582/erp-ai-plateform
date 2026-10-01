package com.erp.business.inventory.items.dto;

import com.erp.business.inventory.domain.ExciseType;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String sku,
        String barcode,
        String hsCode,
        String nameEn,
        String nameNp,
        String descriptionEn,
        String descriptionNp,
        Long categoryId,
        String categoryName,
        Long uomId,
        String uomCode,
        String uomName,
        ItemType itemType,
        TaxabilityType taxabilityType,
        BigDecimal vatRate,
        ExciseType exciseType,
        BigDecimal exciseRate,
        BigDecimal exciseAmount,
        BigDecimal sellingPrice,
        BigDecimal purchasePrice,
        BigDecimal mrp,
        Boolean isVatInclusive,
        Boolean isInventoryTracked,
        Boolean isBatchTracked,
        Boolean isExpiryTracked,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        BigDecimal reorderQuantity,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
