package com.erp.business.inventory.items.dto;

import com.erp.business.inventory.domain.ExciseType;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateItemRequest(
        @Size(min = 1, max = 100, message = "SKU must be between 1 and 100 characters")
        String sku,

        @Size(max = 100, message = "Barcode must not exceed 100 characters")
        String barcode,

        @Size(max = 20, message = "HS Code must not exceed 20 characters")
        String hsCode,

        @Size(min = 1, max = 255, message = "English item name must be between 1 and 255 characters")
        String nameEn,

        @Size(max = 255, message = "Nepali item name must not exceed 255 characters")
        String nameNp,

        String descriptionEn,
        String descriptionNp,

        Long categoryId,
        Long uomId,

        ItemType itemType,
        TaxabilityType taxabilityType,
        BigDecimal vatRate,

        ExciseType exciseType,
        BigDecimal exciseRate,
        BigDecimal exciseAmount,

        @DecimalMin(value = "0.0", message = "Selling price must not be negative")
        BigDecimal sellingPrice,

        BigDecimal purchasePrice,
        BigDecimal mrp,

        Boolean isVatInclusive,
        Boolean isInventoryTracked,
        Boolean isBatchTracked,
        Boolean isExpiryTracked,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        BigDecimal reorderQuantity
) {
}
