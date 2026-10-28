package com.erp.business.sales.invoice.mapper;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceLine;
import com.erp.business.sales.invoice.dto.SalesInvoiceLineResponse;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps the {@link SalesInvoice} aggregate to its read model.
 */
@Component
public class SalesInvoiceMapper {

    public SalesInvoiceResponse toResponse(SalesInvoice invoice) {
        List<SalesInvoiceLineResponse> lineResponses = invoice.getLines().stream()
                .map(this::toLineResponse)
                .toList();

        return new SalesInvoiceResponse(
                invoice.getId(),
                invoice.getTenantId(),
                invoice.getCompanyId(),
                invoice.getInvoiceNumber(),
                invoice.getFiscalYearId(),
                invoice.getBranchId(),
                invoice.getSalesOrderId(),
                invoice.getSoNumber(),
                invoice.getDeliveryId(),
                invoice.getDeliveryNumber(),
                invoice.getCustomerId(),
                invoice.getCustomerCode(),
                invoice.getCustomerName(),
                invoice.getCustomerPanNumber(),
                invoice.getInvoiceDate(),
                invoice.getInvoiceDateNp(),
                invoice.getDueDate(),
                invoice.getIsTaxInvoice(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getSubtotalAmount(),
                invoice.getTaxableAmount(),
                invoice.getExemptAmount(),
                invoice.getZeroRatedAmount(),
                invoice.getVatAmount(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getBalanceAmount(),
                invoice.getNotes(),
                lineResponses,
                invoice.getVersion()
        );
    }

    private SalesInvoiceLineResponse toLineResponse(SalesInvoiceLine line) {
        return new SalesInvoiceLineResponse(
                line.getId(),
                line.getLineNumber(),
                line.getItemId(),
                line.getItemCode(),
                line.getItemName(),
                line.getUomCode(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getDiscountPercentage(),
                line.getVatType(),
                line.getVatRate(),
                line.getLineAmount(),
                line.vatAmount(),
                line.getDescription()
        );
    }
}
