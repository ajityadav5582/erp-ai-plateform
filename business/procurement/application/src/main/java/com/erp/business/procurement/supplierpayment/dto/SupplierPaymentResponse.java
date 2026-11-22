package com.erp.business.procurement.supplierpayment.dto;

import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** A supplier payment, as returned to the client. */
public record SupplierPaymentResponse(
        Long id,
        String paymentNumber,
        Long tenantId,
        Long companyId,
        Long branchId,
        Long fiscalYearId,

        /** The primary invoice if single-invoice payment, or null when multi-invoice. */
        Long purchaseInvoiceId,
        String invoiceNumber,

        LocalDate paymentDate,
        Long supplierId,
        String supplierCode,
        String supplierName,
        BigDecimal amount,
        BigDecimal unallocatedAmount,
        String currency,
        PaymentMethod paymentMethod,
        String referenceNo,
        String paidFrom,
        SupplierPaymentStatus status,
        String notes,
        Instant postedAt,
        String postedBy,
        String cancellationReason,
        Instant createdAt,

        /** The line allocations settled by this payment. */
        List<PaymentAllocationResponse> allocations
) {

    /** Backward-compatible constructor without unallocatedAmount and allocations. */
    public SupplierPaymentResponse(
            Long id,
            String paymentNumber,
            Long tenantId,
            Long companyId,
            Long branchId,
            Long fiscalYearId,
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
        this(
                id,
                paymentNumber,
                tenantId,
                companyId,
                branchId,
                fiscalYearId,
                purchaseInvoiceId,
                invoiceNumber,
                paymentDate,
                supplierId,
                supplierCode,
                supplierName,
                amount,
                BigDecimal.ZERO,
                currency,
                paymentMethod,
                referenceNo,
                paidFrom,
                status,
                notes,
                postedAt,
                postedBy,
                cancellationReason,
                createdAt,
                Collections.emptyList()
        );
    }
}
