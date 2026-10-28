package com.erp.business.sales.invoice;

import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.dto.CreateInvoiceRequest;
import com.erp.business.sales.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceDecisionRequest;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.invoice.dto.TaxInvoiceView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sales (tax) invoice use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface SalesInvoiceService {

    SalesInvoiceResponse createInvoice(Long tenantId, Long companyId, CreateInvoiceRequest request);

    /**
     * Creates and issues a Sales Invoice for a POS transaction. Idempotent by
     * posTransactionId: a retry returns the existing invoice.
     */
    SalesInvoiceResponse createPosInvoice(Long tenantId, Long companyId, CreatePosInvoiceRequest request);

    SalesInvoiceResponse getInvoiceById(Long tenantId, Long companyId, Long id);

    SalesInvoiceResponse getInvoiceByNumber(Long tenantId, Long companyId, String invoiceNumber);

    SalesInvoiceResponse getInvoiceByOrder(Long tenantId, Long companyId, Long salesOrderId);

    Page<SalesInvoiceResponse> listInvoices(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesInvoiceResponse> listInvoicesByStatus(Long tenantId, Long companyId,
                                                    SalesInvoiceStatus status, Pageable pageable);

    Page<SalesInvoiceResponse> listInvoicesByCustomer(Long tenantId, Long companyId,
                                                      Long customerId, Pageable pageable);

    Page<SalesInvoiceResponse> searchInvoices(Long tenantId, Long companyId, String term, Pageable pageable);

    /** Invoices still owing money, for the receivables view. */
    List<SalesInvoiceResponse> listOutstandingInvoices(Long tenantId, Long companyId);

    /** Distinct sales order IDs that already have an active invoice. */
    List<Long> getInvoicedOrderIds(Long tenantId, Long companyId);

    /** Issues a draft invoice to the customer. The IRD-relevant act. */
    SalesInvoiceResponse issueInvoice(Long tenantId, Long companyId, Long id);

    SalesInvoiceResponse cancelInvoice(Long tenantId, Long companyId, Long id,
                                       InvoiceDecisionRequest request);

    /**
     * Applies a customer payment to an issued invoice and re-derives its state.
     * Called by the customer-receipt use case.
     */
    SalesInvoiceResponse recordPayment(Long tenantId, Long companyId, Long id, BigDecimal amount);

    /**
     * Reverses a previously applied payment (a cancelled customer receipt) and
     * re-derives the invoice's state.
     */
    SalesInvoiceResponse reversePayment(Long tenantId, Long companyId, Long id, BigDecimal amount);

    /**
     * Builds the printable IRD tax-invoice view (mandatory fields + QR payload)
     * for an invoice. Seller identity is supplied from the company profile.
     */
    TaxInvoiceView getTaxInvoiceView(Long tenantId, Long companyId, Long id);
}
