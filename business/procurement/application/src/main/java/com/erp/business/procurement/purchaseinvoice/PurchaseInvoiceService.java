package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.dto.PurchaseInvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Records supplier invoices and tracks what is owed.
 *
 * <p>Creating an invoice is what creates the payable. Everything else here - reading,
 * listing what is outstanding - exists so that payable figure can be worked.
 */
public interface PurchaseInvoiceService {

    /**
     * Records a supplier invoice against a delivery.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param request   the delivery, the supplier's invoice number, and the billed lines
     * @return the created invoice
     */
    PurchaseInvoiceResponse create(Long tenantId, Long companyId, CreateInvoiceRequest request);

    PurchaseInvoiceResponse getById(Long tenantId, Long companyId, Long id);

    /** Every invoice in the caller's scope. */
    Page<PurchaseInvoiceResponse> findAll(Long tenantId, Long companyId, Pageable pageable);

    /** Invoices still owing money. The accounts payable ageing worklist. */
    Page<PurchaseInvoiceResponse> findOutstanding(Long tenantId, Long companyId, Pageable pageable);
}
