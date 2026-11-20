package com.erp.business.procurement.purchasereturn.dto;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;

import java.math.BigDecimal;

public record ReturnableLineResponse(
        Long purchaseInvoiceLineId,
        Integer lineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        BigDecimal invoicedQuantity,
        BigDecimal alreadyReturnedQuantity,
        BigDecimal returnableQuantity,
        BigDecimal unitPrice,
        PurchaseTaxCategory taxCategory,
        BigDecimal taxRate
) {}

