package com.erp.business.procurement.purchasereturn.dto;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;

import java.math.BigDecimal;

public record PurchaseReturnLineResponse(
        Long id,
        Integer lineNumber,
        Long purchaseInvoiceLineId,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal returnQuantity,
        BigDecimal unitPrice,
        PurchaseTaxCategory taxCategory,
        BigDecimal taxRate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        String remarks
) {}

