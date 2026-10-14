package com.erp.business.sales.quotation.mapper;

import com.erp.business.sales.quotation.domain.SalesQuotation;
import com.erp.business.sales.quotation.domain.SalesQuotationLine;
import com.erp.business.sales.quotation.dto.SalesQuotationLineResponse;
import com.erp.business.sales.quotation.dto.SalesQuotationResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SalesQuotationMapper {

    public SalesQuotationResponse toResponse(SalesQuotation quotation) {
        List<SalesQuotationLineResponse> lineResponses = quotation.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new SalesQuotationResponse(
                quotation.getId(),
                quotation.getTenantId(),
                quotation.getCompanyId(),
                quotation.getQuotationNumber(),
                quotation.getQuotationDate(),
                quotation.getValidUntilDate(),
                quotation.getCustomerId(),
                quotation.getCustomerCode(),
                quotation.getCustomerName(),
                quotation.getCustomerPanNumber(),
                quotation.getCurrency(),
                quotation.getFiscalYearId(),
                quotation.getBranchId(),
                quotation.getWarehouseId(),
                quotation.getStatus(),
                quotation.getSubtotalAmount(),
                quotation.getDiscountAmount(),
                quotation.getTaxAmount(),
                quotation.getTotalAmount(),
                quotation.getSalesOrderId(),
                quotation.getConvertedAt(),
                quotation.getReference(),
                quotation.getTermsAndConditions(),
                quotation.getNotes(),
                lineResponses
        );
    }

    private SalesQuotationLineResponse toLineResponse(SalesQuotationLine line) {
        return new SalesQuotationLineResponse(
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
                line.getDescription()
        );
    }
}
