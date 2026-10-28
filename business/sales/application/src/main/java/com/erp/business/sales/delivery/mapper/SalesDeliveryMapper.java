package com.erp.business.sales.delivery.mapper;

import com.erp.business.sales.delivery.domain.SalesDelivery;
import com.erp.business.sales.delivery.domain.SalesDeliveryLine;
import com.erp.business.sales.delivery.dto.SalesDeliveryLineResponse;
import com.erp.business.sales.delivery.dto.SalesDeliveryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps the {@link SalesDelivery} aggregate to its read model.
 */
@Component
public class SalesDeliveryMapper {

    public SalesDeliveryResponse toResponse(SalesDelivery delivery) {
        List<SalesDeliveryLineResponse> lineResponses = delivery.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new SalesDeliveryResponse(
                delivery.getId(),
                delivery.getTenantId(),
                delivery.getCompanyId(),
                delivery.getDeliveryNumber(),
                delivery.getFiscalYearId(),
                delivery.getBranchId(),
                delivery.getSalesOrderId(),
                delivery.getSoNumber(),
                delivery.getCustomerId(),
                delivery.getCustomerCode(),
                delivery.getCustomerName(),
                delivery.getWarehouseId(),
                delivery.getWarehouseCode(),
                delivery.getWarehouseName(),
                delivery.getDeliveryDate(),
                delivery.getStatus(),
                delivery.getNotes(),
                lineResponses,
                delivery.getVersion()
        );
    }

    private SalesDeliveryLineResponse toLineResponse(SalesDeliveryLine line) {
        return new SalesDeliveryLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getSalesOrderLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getQuantity(),
                line.getDescription()
        );
    }
}
