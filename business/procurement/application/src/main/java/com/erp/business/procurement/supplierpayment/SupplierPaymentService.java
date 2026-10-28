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
}
