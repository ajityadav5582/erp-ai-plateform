package com.erp.business.sales.salesorder;

import com.erp.business.sales.salesorder.domain.SalesOrderStatus;
import com.erp.business.sales.salesorder.dto.CreateSalesOrderRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderDecisionRequest;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sales order use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface SalesOrderService {

    SalesOrderResponse createOrder(Long tenantId, Long companyId, CreateSalesOrderRequest request);

    SalesOrderResponse updateOrder(Long tenantId, Long companyId, Long id, com.erp.business.sales.salesorder.dto.UpdateSalesOrderRequest request);

    SalesOrderResponse getOrderById(Long tenantId, Long companyId, Long id);

    SalesOrderResponse getOrderByNumber(Long tenantId, Long companyId, String soNumber);

    Page<SalesOrderResponse> listOrders(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesOrderResponse> listOrdersByStatus(Long tenantId, Long companyId,
                                                SalesOrderStatus status, Pageable pageable);

    Page<SalesOrderResponse> listOrdersByCustomer(Long tenantId, Long companyId,
                                                  Long customerId, Pageable pageable);

    Page<SalesOrderResponse> searchOrders(Long tenantId, Long companyId, String term, Pageable pageable);

    List<SalesOrderResponse> listOpenOrders(Long tenantId, Long companyId);

    SalesOrderResponse confirmOrder(Long tenantId, Long companyId, Long id);

    SalesOrderResponse cancelOrder(Long tenantId, Long companyId, Long id, SalesOrderDecisionRequest request);

    SalesOrderResponse closeOrder(Long tenantId, Long companyId, Long id);

    /**
     * Records a delivery against a line and re-derives the header status. Called by
     * the delivery use case when a delivery is posted.
     */
    SalesOrderResponse recordDelivery(Long tenantId, Long companyId, Long orderId,
                                      int lineNumber, BigDecimal quantity);

    /**
     * Reverses a previously recorded delivery (a voided delivery) and re-derives the
     * header status.
     */
    SalesOrderResponse reverseDelivery(Long tenantId, Long companyId, Long orderId,
                                       int lineNumber, BigDecimal quantity);
}
