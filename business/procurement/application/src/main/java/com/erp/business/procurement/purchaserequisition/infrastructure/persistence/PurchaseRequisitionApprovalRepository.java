package com.erp.business.procurement.purchaserequisition.infrastructure.persistence;

import com.erp.business.procurement.purchaserequisition.domain.ApprovalAction;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PurchaseRequisitionApprovalRepository
        extends JpaRepository<PurchaseRequisitionApproval, Long> {

    List<PurchaseRequisitionApproval> findByRequisitionIdOrderBySequenceNoAsc(Long requisitionId);

    /**
     * Approver inbox for a named approver.
     */
    List<PurchaseRequisitionApproval> findByTenantIdAndCompanyIdAndApproverIdAndActionIn(
            Long tenantId, Long companyId, Long approverId, Collection<ApprovalAction> actions);

    /**
     * Pending steps the caller could act on: those naming them directly, plus those
     * naming a role they hold.
     *
     * <p><strong>This returns candidate steps, not an actionable queue.</strong>
     * A submission materialises every applicable step as PENDING at once, so a
     * caller who is the assignee of step 2 sees step 2 here while step 1 is still
     * outstanding - and cannot act on it, because {@code currentStep} only ever
     * returns the lowest pending one. The service narrows this set to the steps
     * that are genuinely current before returning an inbox; the query cannot do it
     * on its own because "is this the lowest pending step for its document" is a
     * per-document question, not a row predicate.
     *
     * <p>The role match is on {@code approverRole}, the name frozen onto the step
     * at submission, for the same reason {@code isRoleHeldBy} matches on it:
     * resolving the role live would let an administrative rename move a step off
     * the person who owes it.
     *
     * <p>{@code roleNames} must not be empty. An empty {@code IN} list is not
     * portable JPQL, so the service substitutes a value that cannot match any real
     * role name; the direct-assignee half of the disjunction still works.
     *
     * @param tenantId  the tenant
     * @param companyId the company
     * @param approverId the acting user id
     * @param roleNames the acting user's role names, guaranteed non-empty
     * @return every pending step naming the user or one of their roles
     */
    @Query("SELECT a FROM PurchaseRequisitionApproval a WHERE a.tenantId = :tenantId " +
           "AND a.companyId = :companyId " +
           "AND a.action = com.erp.business.procurement.purchaserequisition.domain.ApprovalAction.PENDING " +
           "AND (a.approverId = :approverId OR a.approverRole IN :roleNames) " +
           "ORDER BY a.requisition.submittedAt ASC, a.sequenceNo ASC")
    List<PurchaseRequisitionApproval> findActionableStepsFor(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("approverId") Long approverId,
            @Param("roleNames") List<String> roleNames);

    /**
     * The single step an approver may act on: lowest sequence still pending.
     */
    @Query("SELECT a FROM PurchaseRequisitionApproval a WHERE a.tenantId = :tenantId " +
           "AND a.companyId = :companyId AND a.requisition.id = :requisitionId " +
           "AND a.action = com.erp.business.procurement.purchaserequisition.domain.ApprovalAction.PENDING " +
           "ORDER BY a.sequenceNo ASC")
    List<PurchaseRequisitionApproval> findPendingSteps(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("requisitionId") Long requisitionId);
}
