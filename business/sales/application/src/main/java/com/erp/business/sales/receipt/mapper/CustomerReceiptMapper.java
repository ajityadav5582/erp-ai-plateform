package com.erp.business.sales.receipt.mapper;

import com.erp.business.sales.receipt.domain.CustomerReceipt;
import com.erp.business.sales.receipt.dto.CustomerReceiptResponse;
import org.springframework.stereotype.Component;

/**
 * Maps the {@link CustomerReceipt} aggregate to its read model.
 */
@Component
public class CustomerReceiptMapper {

    public CustomerReceiptResponse toResponse(CustomerReceipt receipt) {
        return new CustomerReceiptResponse(
                receipt.getId(),
                receipt.getTenantId(),
                receipt.getCompanyId(),
                receipt.getReceiptNumber(),
                receipt.getFiscalYearId(),
                receipt.getBranchId(),
                receipt.getInvoiceId(),
                receipt.getInvoiceNumber(),
                receipt.getCustomerId(),
                receipt.getCustomerCode(),
                receipt.getCustomerName(),
                receipt.getAmount(),
                receipt.getPaymentMethod(),
                receipt.getReceiptDate(),
                receipt.getReference(),
                receipt.getNotes(),
                receipt.getStatus(),
                receipt.getVersion()
        );
    }
}
