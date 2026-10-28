package com.erp.business.procurement.purchaserequisition;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApproval;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLine;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatusHistory;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionDetailResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionLineResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionListResponse;
import org.springframework.stereotype.Component;

/**
 * Maps requisition aggregates to their read-model DTOs.
 *
 * <p>Extracted from {@link PurchaseRequisitionServiceImpl} so the service holds
 * use cases and this holds presentation shaping. The mapping is pure - it reads
 * entity getters and calls the aggregate's own derived-value methods
 * ({@code outstandingValue()}, {@code shortfallQuantity()}, the approved-amount
 * helpers) - with no repository or service access, which is what makes it safe to
 * unit test on its own and to change without touching workflow logic.
 */
@Component
public class RequisitionResponseMapper {

    public RequisitionListResponse toList(PurchaseRequisition requisition) {
        return new RequisitionListResponse(
                requisition.getId(),
                requisition.getPrNumber(),
                requisition.getBranchId(),
                requisition.getDepartmentId(),
                requisition.getRequestedBy(),
                requisition.getRequestDate(),
                requisition.getRequestDateNp(),
                requisition.getRequiredByDate(),
                requisition.getRequiredByDateNp(),
                requisition.getStatus(),
                requisition.getPriority(),
                requisition.getCurrency(),
                requisition.getEstimatedTotalAmount(),
                requisition.getApprovedTotalAmount(),
                requisition.getOrderedTotalAmount(),
                requisition.outstandingValue(),
                requisition.getSubmittedAt(),
                requisition.getCreatedAt());
    }

    public RequisitionDetailResponse toDetail(PurchaseRequisition requisition) {
        return new RequisitionDetailResponse(
                requisition.getId(),
                requisition.getPrNumber(),
                requisition.getTenantId(),
                requisition.getCompanyId(),
                requisition.getBranchId(),
                requisition.getDepartmentId(),
                requisition.getFiscalYearId(),
                requisition.getRequestedBy(),
                requisition.getRequestDate(),
                requisition.getRequestDateNp(),
                requisition.getRequestDateNpYear(),
                requisition.getRequiredByDate(),
                requisition.getRequiredByDateNp(),
                requisition.getStatus(),
                requisition.getPriority(),
                requisition.getCurrency(),
                requisition.getEstimatedSubtotalAmount(),
                requisition.getEstimatedTaxAmount(),
                requisition.getEstimatedTotalAmount(),
                requisition.getApprovedTotalAmount(),
                requisition.getOrderedTotalAmount(),
                requisition.outstandingValue(),
                requisition.getWarehouseId(),
                requisition.getWarehouseCode(),
                requisition.getWarehouseName(),
                requisition.getJustification(),
                requisition.getNotes(),
                requisition.getSubmittedAt(),
                requisition.getSubmittedBy(),
                requisition.getApprovedAt(),
                requisition.getApprovedBy(),
                requisition.getRejectedAt(),
                requisition.getRejectedBy(),
                requisition.getRejectionReason(),
                requisition.getConvertedAt(),
                requisition.getConvertedBy(),
                requisition.getCancelledAt(),
                requisition.getCancelledBy(),
                requisition.getCancellationReason(),
                requisition.getVersion(),
                requisition.getLines().stream().map(this::toLine).toList(),
                requisition.getApprovals().stream().map(this::toApproval).toList(),
                requisition.getStatusHistory().stream().map(this::toHistory).toList());
    }

    public RequisitionLineResponse toLine(PurchaseRequisitionLine line) {
        return new RequisitionLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getUnitId(),
                line.getRequestedQuantity(),
                line.getApprovedQuantity(),
                line.getOrderedQuantity(),
                line.shortfallQuantity(),
                line.getEstimatedUnitPrice(),
                line.getTaxRate(),
                line.approvedNetAmount(),
                line.approvedTaxAmount(),
                line.approvedGrossAmount(),
                line.getStatus(),
                line.getRemarks());
    }

    public RequisitionDetailResponse.ApprovalStepResponse toApproval(PurchaseRequisitionApproval approval) {
        return new RequisitionDetailResponse.ApprovalStepResponse(
                approval.getId(),
                approval.getSequenceNo(),
                approval.getApproverId(),
                approval.getApproverRoleId(),
                approval.getApproverRole(),
                approval.getAmountThreshold(),
                approval.getAction(),
                approval.getActedBy(),
                approval.getActedAt(),
                approval.getComments());
    }

    public RequisitionDetailResponse.RequisitionHistoryResponse toHistory(PurchaseRequisitionStatusHistory history) {
        return new RequisitionDetailResponse.RequisitionHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangedAt(),
                history.getReason());
    }
}
