package com.erp.business.procurement.supplierpayment;

import com.erp.business.procurement.supplierpayment.dto.CreatePaymentRequest;
import com.erp.business.procurement.supplierpayment.dto.SupplierPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Pays suppliers against recorded invoices. */
public interface SupplierPaymentService {

    /**
     * Records a payment and settles it against the invoice.
     *
     * <p>Both happen in one transaction. A payment that exists without reducing the
     * payable leaves the company looking settled to a reader of the payment list and
     * still in arrears to a reader of the invoice.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param request   the invoice, the amount, and how the money was sent
     * @param actor     the acting user id
     * @return the created payment
     */
    SupplierPaymentResponse pay(Long tenantId, Long companyId,
                                CreatePaymentRequest request, String actor);

    SupplierPaymentResponse getById(Long tenantId, Long companyId, Long id);

    /** Every payment in the caller's scope. */
    Page<SupplierPaymentResponse> findAll(Long tenantId, Long companyId, Pageable pageable);

    /** The payments made against one invoice. */
    Page<SupplierPaymentResponse> findByInvoice(Long tenantId, Long companyId, Long invoiceId,
                                                Pageable pageable);

    /**
     * Lists payments with available unallocated advance balance for a supplier.
     */
    java.util.List<SupplierPaymentResponse> findAvailableAdvances(
            Long tenantId, Long companyId, Long supplierId);

    /**
     * Applies / offsets available advance credit against an approved purchase invoice.
     */
    SupplierPaymentResponse applyAdvance(
            Long tenantId, Long companyId,
            com.erp.business.procurement.supplierpayment.dto.ApplyAdvanceRequest request,
            String actor);

    /**
     * Real-time balance summary for a supplier: total outstanding payables across unpaid
     * and partially-paid invoices, and total unallocated advance payments available.
     */
    com.erp.business.procurement.supplierpayment.dto.SupplierBalanceSummaryResponse getSupplierBalanceSummary(
            Long tenantId, Long companyId, Long supplierId);

    /**
     * Chronological running Statement of Account for a supplier, including opening balance,
     * purchase invoices, payments, debit notes, and running balance.
     */
    com.erp.business.procurement.supplierpayment.dto.SupplierStatementResponse getSupplierStatement(
            Long tenantId, Long companyId, Long supplierId,
            java.time.LocalDate fromDate, java.time.LocalDate toDate);
}
