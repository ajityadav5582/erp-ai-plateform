package com.erp.business.sales.receipt;

import com.erp.business.sales.receipt.domain.ReceiptStatus;
import com.erp.business.sales.receipt.dto.CreateReceiptRequest;
import com.erp.business.sales.receipt.dto.CustomerReceiptResponse;
import com.erp.business.sales.receipt.dto.ReceiptDecisionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Customer receipt use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface CustomerReceiptService {

    CustomerReceiptResponse createReceipt(Long tenantId, Long companyId, CreateReceiptRequest request);

    CustomerReceiptResponse getReceiptById(Long tenantId, Long companyId, Long id);

    CustomerReceiptResponse getReceiptByNumber(Long tenantId, Long companyId, String receiptNumber);

    Page<CustomerReceiptResponse> listReceipts(Long tenantId, Long companyId, Pageable pageable);

    Page<CustomerReceiptResponse> listReceiptsByStatus(Long tenantId, Long companyId,
                                                       ReceiptStatus status, Pageable pageable);

    Page<CustomerReceiptResponse> listReceiptsByInvoice(Long tenantId, Long companyId,
                                                        Long invoiceId, Pageable pageable);

    Page<CustomerReceiptResponse> listReceiptsByCustomer(Long tenantId, Long companyId,
                                                         Long customerId, Pageable pageable);

    Page<CustomerReceiptResponse> searchReceipts(Long tenantId, Long companyId, String term, Pageable pageable);

    /**
     * Cancels a posted receipt: reverses the payment against the invoice, in one
     * transaction.
     */
    CustomerReceiptResponse cancelReceipt(Long tenantId, Long companyId, Long id,
                                          ReceiptDecisionRequest request);
}
