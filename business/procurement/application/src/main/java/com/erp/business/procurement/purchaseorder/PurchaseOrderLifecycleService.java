package com.erp.business.procurement.purchaseorder;

import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderDecisionRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;

/**
 * Moves a purchase order along its lifecycle.
 *
 * <p><strong>Why this exists separately from conversion.</strong> Conversion creates a
 * DRAFT. Without the transitions below, nothing could move it any further: goods
 * receipt requires {@code APPROVED}, and {@code APPROVED} is only reachable through
 * submit then approve. The flow stopped at the second step of the purchase process.
 *
 * <p>Each method is a thin, honest wrapper. The permitted transitions live on
 * {@code PurchaseOrderStatus.canTransitionTo} and the workflow columns are stamped by
 * {@code PurchaseOrder.transitionTo}; this layer only loads the right aggregate, calls
 * that, and saves. Duplicating the state machine here would be the one way to make the
 * two disagree.
 */
public interface PurchaseOrderLifecycleService {

    /**
     * Submits a draft for approval, freezing its content.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @param actor     the acting user id
     * @return the order in SUBMITTED
     */
    PurchaseOrderResponse submit(Long tenantId, Long companyId, Long id, String actor);

    /**
     * Approves a submitted order, issuing it to the supplier.
     *
     * <p>This is the gate that makes the goods receivable. Only an APPROVED order accepts
     * a goods receipt note.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @param actor     the acting user id
     * @return the order in APPROVED
     */
    PurchaseOrderResponse approve(Long tenantId, Long companyId, Long id, String actor);

    /**
     * Refuses a submitted order. A reason is mandatory.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @param request   carries the rejection reason
     * @param actor     the acting user id
     * @return the order in REJECTED
     */
    PurchaseOrderResponse reject(Long tenantId, Long companyId, Long id,
                                 PurchaseOrderDecisionRequest request, String actor);

    /**
     * Voids an order before or during receipt. A reason is mandatory.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @param request   carries the cancellation reason
     * @param actor     the acting user id
     * @return the order in CANCELLED
     */
    PurchaseOrderResponse cancel(Long tenantId, Long companyId, Long id,
                                 PurchaseOrderDecisionRequest request, String actor);

    /**
     * Closes a fully received order once its invoices are settled.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @param actor     the acting user id
     * @return the order in CLOSED
     */
    PurchaseOrderResponse close(Long tenantId, Long companyId, Long id, String actor);
}
