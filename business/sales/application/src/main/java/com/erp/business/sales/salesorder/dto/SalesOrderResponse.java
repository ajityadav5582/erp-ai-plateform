package com.erp.business.sales.salesorder.dto;

import com.erp.business.sales.salesorder.domain.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model for a sales order, returned by every sales-order endpoint.
 *
 * <p>A flat record with a nested line list rather than the entity, so the
 * persistence type never leaks past the service boundary.
 */
public record SalesOrderResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String soNumber,
        Long fiscalYearId,
        Long branchId,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        Long customerId,
        String customerCode,
        String customerName,
        String customerPanNumber,
        SalesOrderStatus status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal deliveredAmount,
        Integer paymentTermsDays,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String reference,
        String notes,
        List<SalesOrderLineResponse> lines,
        Integer version
) {
}
