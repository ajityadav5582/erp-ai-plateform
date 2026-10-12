package com.erp.business.procurement.purchaserequisition;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import com.erp.business.procurement.purchaserequisition.dto.ApprovalDecisionRequest;
import com.erp.business.procurement.purchaserequisition.dto.CreateRequisitionRequest;
import com.erp.business.procurement.purchaserequisition.dto.OrderAwardRequest;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionActionabilityResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionDetailResponse;
import com.erp.business.procurement.purchaserequisition.dto.RequisitionListResponse;
import com.erp.business.procurement.purchaserequisition.dto.UpdateRequisitionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseRequisitionService {

    RequisitionDetailResponse create(Long tenantId, Long companyId, Long requestedBy,
                                     CreateRequisitionRequest request);

    RequisitionDetailResponse getById(Long tenantId, Long companyId, Long id);

    Page<RequisitionListResponse> list(Long tenantId, Long companyId,
                                       Long branchId, PurchaseRequisitionStatus status,
                                       Pageable pageable);

    /**
     * Replaces the content of a draft or returned requisition.
     *
     * <p>The whole document is replaced rather than patched, because the client
     * edits a grid of lines and submits it: a partial update would have to define
     * what "absent" means for every field, and get it wrong in a way that silently
     * deletes data.
     *
     * <p>Branch, request date and requester are not editable. Branch decides which
     * approval policy applies, so re-pointing it would change the control the
     * document is subject to; a requester who raised this for the wrong branch
     * should cancel and re-raise, which leaves an honest trail of the mistake.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the requisition id
     * @param request   the replacement content
     * @param actor     the acting user id
     * @return the updated document
     */
    RequisitionDetailResponse update(Long tenantId, Long companyId, Long id,
                                     UpdateRequisitionRequest request, String actor);

    /**
     * Requisitions this user raised, newest first.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param requestedBy the user id
     * @param pageable  paging
     * @return the caller's own requisitions in this company
     */
    Page<RequisitionListResponse> listMine(Long tenantId, Long companyId, Long requestedBy,
                                           Pageable pageable);

    /**
     * Requisitions whose current approval step names this user or a role they hold.
     *
     * <p>"Current" is load-bearing: a submission materialises every applicable step
     * as pending at once, so a caller who is the assignee of step 2 must not be
     * shown a document that is still waiting on step 1.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param approverId the acting user id
     * @param pageable  paging
     * @return the caller's actionable queue, oldest submission first
     */
    Page<RequisitionListResponse> approvalInbox(Long tenantId, Long companyId, Long approverId,
                                                Pageable pageable);

    /**
     * What this user may do with one requisition right now, and why not when they
     * may not.
     *
     * <p>Advisory for rendering. Every flag is re-derived and enforced on the
     * endpoint that performs the action, because a hint the caller can forge is not
     * a control.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the requisition id
     * @param actor     the acting user id
     * @return the actionability report
     */
    RequisitionActionabilityResponse actionability(Long tenantId, Long companyId, Long id,
                                                   String actor);

    RequisitionDetailResponse submit(Long tenantId, Long companyId, Long id, String actor);

    RequisitionDetailResponse decide(Long tenantId, Long companyId, Long id,
                                     ApprovalDecisionRequest request, String actor);

    RequisitionDetailResponse awardToOrder(Long tenantId, Long companyId, Long id,
                                           OrderAwardRequest request, String actor);

    RequisitionDetailResponse cancel(Long tenantId, Long companyId, Long id,
                                     String reason, String actor);

    /**
     * Deletes a draft requisition.
     *
     * <p>Takes the acting user because deletion is an ownership-controlled
     * operation: holding {@code PURCHASE_DELETE} authorises the operation, but the
     * caller must also be the user who raised the document unless they hold
     * {@code PURCHASE_ADMIN}.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the requisition id
     * @param actor     the acting user id
     */
    void delete(Long tenantId, Long companyId, Long id, String actor);
}
