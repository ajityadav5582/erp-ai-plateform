package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLineStatus;

import java.math.BigDecimal;

public record RequisitionLineResponse(

        Long id,
        Integer lineNumber,
        Long itemId,
        String itemCode,
        String itemName,
        String uomCode,
        Long unitId,
        BigDecimal requestedQuantity,
        BigDecimal approvedQuantity,
        BigDecimal orderedQuantity,
        BigDecimal shortfallQuantity,
        BigDecimal estimatedUnitPrice,
        BigDecimal taxRate,
        BigDecimal approvedNetAmount,
        BigDecimal approvedTaxAmount,
        BigDecimal approvedGrossAmount,
        PurchaseRequisitionLineStatus status,
        String remarks
) {
}
