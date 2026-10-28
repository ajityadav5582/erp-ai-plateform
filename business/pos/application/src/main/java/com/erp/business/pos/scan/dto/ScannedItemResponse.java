package com.erp.business.pos.scan.dto;

import com.erp.business.pos.domain.PosVatType;

import java.math.BigDecimal;

/**
 * Everything the POS terminal needs after a scan: the item snapshot plus its
 * resolved price and VAT, so the UI can add the line and show totals without a
 * second call.
 */
public record ScannedItemResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String uomCode,
        BigDecimal unitPrice,
        PosVatType vatType,
        BigDecimal vatRate,
        Long availableStock,
        Boolean isStockable
) {
}
