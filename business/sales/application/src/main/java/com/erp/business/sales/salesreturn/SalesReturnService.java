package com.erp.business.sales.salesreturn;

import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.dto.CreateReturnRequest;
import com.erp.business.sales.salesreturn.dto.ReturnDecisionRequest;
import com.erp.business.sales.salesreturn.dto.SalesReturnResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Sales return (credit note) use cases.
 *
 * <p>Every method is tenant- and company-scoped by its leading parameters; the
 * controller supplies them from the authenticated request context, never from the
 * payload. Authorisation is declared with {@code @RequiresPermission} on the
 * implementation and enforced by the shared aspect before the body runs.
 */
public interface SalesReturnService {

    SalesReturnResponse createReturn(Long tenantId, Long companyId, CreateReturnRequest request);

    SalesReturnResponse getReturnById(Long tenantId, Long companyId, Long id);

    SalesReturnResponse getReturnByNumber(Long tenantId, Long companyId, String creditNoteNumber);

    Page<SalesReturnResponse> listReturns(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesReturnResponse> listReturnsByStatus(Long tenantId, Long companyId,
                                                  ReturnStatus status, Pageable pageable);

    Page<SalesReturnResponse> listReturnsByInvoice(Long tenantId, Long companyId,
                                                   Long invoiceId, Pageable pageable);

    Page<SalesReturnResponse> searchReturns(Long tenantId, Long companyId, String term, Pageable pageable);

    /**
     * Posts a credit note: restocks the returned goods in Inventory, in one
     * transaction.
     */
    SalesReturnResponse postReturn(Long tenantId, Long companyId, Long id);

    SalesReturnResponse cancelReturn(Long tenantId, Long companyId, Long id,
                                     ReturnDecisionRequest request);
}
