package com.erp.business.sales.salesorder.mapper;

import com.erp.business.sales.salesorder.domain.SalesOrder;
import com.erp.business.sales.salesorder.domain.SalesOrderLine;
import com.erp.business.sales.salesorder.dto.SalesOrderLineResponse;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps the {@link SalesOrder} aggregate to its read model.
 *
 * <p>Hand-written rather than reflection-based: an explicit mapping fails to
 * compile when a field is added, instead of silently dropping it from the response.
 */
@Component
public class SalesOrderMapper {

    public SalesOrderResponse toResponse(SalesOrder order) {
        List<SalesOrderLineResponse> lineResponses = order.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new SalesOrderResponse(
                order.getId(),
                order.getTenantId(),
                order.getCompanyId(),
                order.getSoNumber(),
                order.getFiscalYearId(),
                order.getBranchId(),
                order.getOrderDate(),
                order.getExpectedDeliveryDate(),
                order.getCustomerId(),
                order.getCustomerCode(),
                order.getCustomerName(),
                order.getCustomerPanNumber(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getDeliveredAmount(),
                order.getPaymentTermsDays(),
                order.getWarehouseId(),
                order.getWarehouseCode(),
                order.getWarehouseName(),
                order.getReference(),
                order.getNotes(),
                lineResponses,
                order.getVersion()
        );
    }

    private SalesOrderLineResponse toLineResponse(SalesOrderLine line) {
        return new SalesOrderLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getDiscountPercentage(),
                line.getTaxRate(),
                line.getLineAmount(),
                line.getDeliveredQuantity(),
                line.outstandingQuantity(),
                line.getStatus(),
                line.getDescription()
        );
    }
}
