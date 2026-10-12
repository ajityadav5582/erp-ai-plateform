package com.erp.business.procurement.supplierpayment.dto;

import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** A supplier payment, as returned to the client. */
public record SupplierPaymentResponse(
        Long id,
        String paymentNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long fiscalYearId,

        /** The invoice this payment settles. */
        Long purchaseInvoiceId,
        String invoiceNumber,

        LocalDate paymentDate,
        Long supplierId,
        String supplierCode,
        String supplierName,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String referenceNo,
        String paidFrom,
        SupplierPaymentStatus status,
        String notes,
        Instant postedAt,
        String postedBy,
        String cancellationReason,
        Instant createdAt
) {
}
