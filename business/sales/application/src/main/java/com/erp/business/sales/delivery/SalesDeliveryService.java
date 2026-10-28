package com.erp.business.sales.delivery;

import com.erp.business.sales.delivery.domain.DeliveryStatus;
import com.erp.business.sales.delivery.dto.CreateDeliveryRequest;
import com.erp.business.sales.delivery.dto.DeliveryDecisionRequest;
import com.erp.business.sales.delivery.dto.SalesDeliveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Sales delivery (goods issue) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface SalesDeliveryService {

    SalesDeliveryResponse createDelivery(Long tenantId, Long companyId, CreateDeliveryRequest request);

    SalesDeliveryResponse getDeliveryById(Long tenantId, Long companyId, Long id);

    SalesDeliveryResponse getDeliveryByNumber(Long tenantId, Long companyId, String deliveryNumber);

    Page<SalesDeliveryResponse> listDeliveries(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesDeliveryResponse> listDeliveriesByStatus(Long tenantId, Long companyId,
                                                       DeliveryStatus status, Pageable pageable);

    Page<SalesDeliveryResponse> listDeliveriesByOrder(Long tenantId, Long companyId,
                                                      Long salesOrderId, Pageable pageable);

    Page<SalesDeliveryResponse> searchDeliveries(Long tenantId, Long companyId, String term, Pageable pageable);

    /**
     * Posts a delivery: issues the outbound stock in Inventory and advances the
     * sales order's delivery progress, all inside one transaction.
     */
    SalesDeliveryResponse postDelivery(Long tenantId, Long companyId, Long id);

    /**
     * Cancels a posted delivery: reverses the outbound stock and rolls back the
     * order's delivery progress.
     */
    SalesDeliveryResponse cancelDelivery(Long tenantId, Long companyId, Long id,
                                         DeliveryDecisionRequest request);
}
