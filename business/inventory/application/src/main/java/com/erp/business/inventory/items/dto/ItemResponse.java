package com.erp.business.inventory.items.dto;

import com.erp.business.inventory.domain.ExciseType;
import com.erp.business.inventory.domain.ItemType;
import com.erp.business.inventory.domain.TaxabilityType;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        @JsonProperty("isVatInclusive") Boolean isVatInclusive,
        @JsonProperty("isInventoryTracked") Boolean isInventoryTracked,
        @JsonProperty("isBatchTracked") Boolean isBatchTracked,
        @JsonProperty("isExpiryTracked") Boolean isExpiryTracked,
        BigDecimal minStockLevel,
        BigDecimal reorderLevel,
        BigDecimal reorderQuantity,
        /** Whether this item may appear on a Purchase Order or Requisition. */
        @JsonProperty("isPurchasable") Boolean isPurchasable,
        /** Whether this item may appear on a Sales Invoice, Sales Order, or POS transaction. */
        @JsonProperty("isSellable") Boolean isSellable,
        /** Whether this item may be used as a Recipe / BOM component. */
        @JsonProperty("isComponent") Boolean isComponent,
        /** Whether this item is the finished-product output of a Recipe / BOM. */
        @JsonProperty("isManufactured") Boolean isManufactured,
        @JsonProperty("isActive") Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Integer version
) {
}
