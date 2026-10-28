package com.erp.business.sales.delivery.dto;

import com.erp.business.sales.delivery.domain.DeliveryStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Read model for a sales delivery, returned by every delivery endpoint.
 */
public record SalesDeliveryResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String deliveryNumber,
        Long fiscalYearId,
        Long branchId,
        Long salesOrderId,
        String soNumber,
        Long customerId,
        String customerCode,
        String customerName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        LocalDate deliveryDate,
        DeliveryStatus status,
        String notes,
        List<SalesDeliveryLineResponse> lines,
        Integer version
) {
}
