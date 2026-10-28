package com.erp.business.procurement.purchaserequisition.infrastructure.persistence;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionApprovalPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequisitionApprovalPolicyRepository
        extends JpaRepository<PurchaseRequisitionApprovalPolicy, Long> {

    /**
     * Branch-specific chain, in order. Resolved first.
     */
    List<PurchaseRequisitionApprovalPolicy> findByTenantIdAndCompanyIdAndBranchIdAndActiveTrueOrderBySequenceNoAsc(
            Long tenantId, Long companyId, Long branchId);

    /**
     * Company-wide default chain, used when the branch has no policy of its own.
     */
    List<PurchaseRequisitionApprovalPolicy> findByTenantIdAndCompanyIdAndBranchIdIsNullAndActiveTrueOrderBySequenceNoAsc(
            Long tenantId, Long companyId);

    Optional<PurchaseRequisitionApprovalPolicy> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    List<PurchaseRequisitionApprovalPolicy> findByTenantIdAndCompanyIdOrderByBranchIdAscSequenceNoAsc(
            Long tenantId, Long companyId);

    /**
     * Does this company define any branch-scoped approval policy at all?
     *
     * <p>Used as a guard before falling back to the company-wide default chain. Without it,
     * naming a branch that does not exist (or that simply has no policy) silently downgrades
     * the requisition to the looser company default — which would let a requester bypass a
     * branch's stricter chain by pointing the requisition at a branch id that is not theirs.
     *
     * <p>Branch existence itself is owned by the identity service and cannot be checked here;
     * this query only distinguishes "company has no branch policies at all" (fall back freely)
     * from "company has branch policies, this branch is not one of them" (reject).
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END "
            + "FROM PurchaseRequisitionApprovalPolicy p "
            + "WHERE p.tenantId = :tenantId AND p.companyId = :companyId "
            + "AND p.branchId IS NOT NULL AND p.active = true")
    boolean existsBranchScopedPolicy(@Param("tenantId") Long tenantId,
                                     @Param("companyId") Long companyId);
}
