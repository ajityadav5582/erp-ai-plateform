package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Summary shape for list views. Lines are excluded so a page of requisitions
 * does not drag the whole line table into the response.
 */
public record RequisitionListResponse(

        Long id,
        String prNumber,
        Long branchId,
        Long departmentId,
        Long requestedBy,
        LocalDate requestDate,
        String requestDateNp,
        LocalDate requiredByDate,
        String requiredByDateNp,
        PurchaseRequisitionStatus status,
        RequisitionPriority priority,
        String currency,
        BigDecimal estimatedTotalAmount,
        BigDecimal approvedTotalAmount,
        BigDecimal orderedTotalAmount,
        BigDecimal outstandingValue,
        Instant submittedAt,
        Instant createdAt
) {
}
