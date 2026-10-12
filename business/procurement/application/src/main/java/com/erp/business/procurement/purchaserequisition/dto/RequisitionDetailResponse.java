package com.erp.business.procurement.purchaserequisition.dto;

import com.erp.business.procurement.purchaserequisition.domain.ApprovalAction;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.domain.RequisitionPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequisitionDetailResponse(

        Long id,
        String prNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long departmentId,
        Long fiscalYearId,
        Long requestedBy,
        LocalDate requestDate,
        String requestDateNp,
        Integer requestDateNpYear,
        LocalDate requiredByDate,
        String requiredByDateNp,
        PurchaseRequisitionStatus status,
        RequisitionPriority priority,
        String currency,
        BigDecimal estimatedSubtotalAmount,
        BigDecimal estimatedTaxAmount,
        BigDecimal estimatedTotalAmount,
        BigDecimal approvedTotalAmount,
        BigDecimal orderedTotalAmount,
        BigDecimal outstandingValue,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String justification,
        String notes,
        Instant submittedAt,
        String submittedBy,
        Instant approvedAt,
        String approvedBy,
        Instant rejectedAt,
        String rejectedBy,
        String rejectionReason,
        Instant convertedAt,
        String convertedBy,
        Instant cancelledAt,
        String cancelledBy,
        String cancellationReason,
        Integer version,
        List<RequisitionLineResponse> lines,
        List<ApprovalStepResponse> approvals,
        List<RequisitionHistoryResponse> statusHistory
) {

    public record ApprovalStepResponse(
            Long id,
            Integer sequenceNo,
            Long approverId,
            Long approverRoleId,
            String approverRole,
            BigDecimal amountThreshold,
            ApprovalAction action,
            String actedBy,
            Instant actedAt,
            String comments
    ) {
    }

    public record RequisitionHistoryResponse(
            Long id,
            PurchaseRequisitionStatus fromStatus,
            PurchaseRequisitionStatus toStatus,
            String changedBy,
            Instant changedAt,
            String reason
    ) {
    }
}
