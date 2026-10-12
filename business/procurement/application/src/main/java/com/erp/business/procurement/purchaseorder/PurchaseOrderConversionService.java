package com.erp.business.procurement.purchaseorder;

import com.erp.business.procurement.purchaseorder.dto.ConvertToPurchaseOrderRequest;
import com.erp.business.procurement.purchaseorder.dto.PurchaseOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Turns an approved purchase requisition into a draft purchase order.
 *
 * <p>Separate from {@code PurchaseRequisitionService.awardToOrder}, which awards
 * demand to an order that already exists. Conversion is the opposite direction: it
 * creates the order. Keeping them apart means the creation path owns the numbering
 * and the duplicate guard, and the award path owns nothing but quantity movement.
 */
public interface PurchaseOrderConversionService {

    /**
     * Creates a draft order from an approved requisition and marks the requisition
     * converted.
     *
     * <p>Copies item, UOM, approved quantity, planning price and tax rate from each
     * orderable requisition line, and copies currency, branch and fiscal year onto
     * the header. Tenant and company are taken from the caller's authenticated scope,
     * never from the request.
     *
     * @param tenantId   the owning tenant
     * @param companyId  the owning company
     * @param request    the source requisition and the supplier to order from
     * @param actor      the acting user id
     * @return the created draft order
     * @throws com.erp.platform.common.exception.ResourceNotFoundException when the
     *         requisition does not exist in this scope
     * @throws com.erp.platform.common.exception.ValidationException when the
     *         requisition is not APPROVED, has already been converted, or carries
     *         no orderable line
     */
    PurchaseOrderResponse convert(Long tenantId, Long companyId,
                                   ConvertToPurchaseOrderRequest request, String actor);

    /**
     * Loads a purchase order in the caller's scope.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param id        the order id
     * @return the order
     */
    PurchaseOrderResponse getById(Long tenantId, Long companyId, Long id);

    /**
     * The order book: every order in the caller's scope.
     *
     * <p>Lives beside {@link #convert} rather than in a separate query service because it
     * is the same document and is read through the same {@code PurchaseOrderAssembler};
     * a second mapper for the same aggregate is how the response to a create call and
     * the response to a later list quietly start disagreeing.
     *
     * <p>Tenant and company come from the caller's authenticated scope, never from the
     * request, so a caller cannot widen the list by editing a parameter.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param pageable  paging and sorting
     * @return the orders in this scope
     */
    Page<PurchaseOrderResponse> findAll(Long tenantId, Long companyId, Pageable pageable);

    /**
     * The orders in the caller's scope still owing goods.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param pageable  paging and sorting
     * @return orders not yet fully received
     */
    Page<PurchaseOrderResponse> findOutstanding(Long tenantId, Long companyId, Pageable pageable);
}
