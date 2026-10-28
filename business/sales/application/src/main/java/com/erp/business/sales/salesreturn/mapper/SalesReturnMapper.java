package com.erp.business.sales.salesreturn.mapper;

import com.erp.business.sales.salesreturn.domain.SalesReturn;
import com.erp.business.sales.salesreturn.domain.SalesReturnLine;
import com.erp.business.sales.salesreturn.dto.SalesReturnLineResponse;
import com.erp.business.sales.salesreturn.dto.SalesReturnResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps the {@link SalesReturn} aggregate to its read model.
 */
@Component
public class SalesReturnMapper {

    public SalesReturnResponse toResponse(SalesReturn salesReturn) {
        List<SalesReturnLineResponse> lineResponses = salesReturn.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new SalesReturnResponse(
                salesReturn.getId(),
                salesReturn.getTenantId(),
                salesReturn.getCompanyId(),
                salesReturn.getCreditNoteNumber(),
                salesReturn.getFiscalYearId(),
                salesReturn.getBranchId(),
                salesReturn.getInvoiceId(),
                salesReturn.getInvoiceNumber(),
                salesReturn.getSalesOrderId(),
                salesReturn.getSoNumber(),
                salesReturn.getCustomerId(),
                salesReturn.getCustomerCode(),
                salesReturn.getCustomerName(),
                salesReturn.getCustomerPanNumber(),
                salesReturn.getWarehouseId(),
                salesReturn.getReturnDate(),
                salesReturn.getReturnDateNp(),
                salesReturn.getReason(),
                salesReturn.getStatus(),
                salesReturn.getSubtotalAmount(),
                salesReturn.getVatAmount(),
                salesReturn.getTotalAmount(),
                lineResponses,
                salesReturn.getVersion()
        );
    }

    private SalesReturnLineResponse toLineResponse(SalesReturnLine line) {
        return new SalesReturnLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getVatType(),
                line.getVatRate(),
                line.getLineAmount(),
                line.vatAmount(),
                line.getDescription()
        );
    }
}
