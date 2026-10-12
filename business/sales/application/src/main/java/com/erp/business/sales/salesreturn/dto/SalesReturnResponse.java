package com.erp.business.sales.salesreturn.dto;

import com.erp.business.sales.salesreturn.domain.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read model for a sales return (credit note).
 */
public record SalesReturnResponse(
        Long id,
        Long tenantId,
        Long companyId,
        String creditNoteNumber,
        Long fiscalYearId,
        Long branchId,
        Long invoiceId,
        String invoiceNumber,
        Long salesOrderId,
        String soNumber,
        Long customerId,
        String customerCode,
        String customerName,
        String customerPanNumber,
        Long warehouseId,
        LocalDate returnDate,
        String returnDateNp,
        String reason,
        ReturnStatus status,
        BigDecimal subtotalAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        List<SalesReturnLineResponse> lines,
        Integer version
) {
}
