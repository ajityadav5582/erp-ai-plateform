package com.erp.business.sales.quotation;

import com.erp.business.sales.quotation.domain.SalesQuotationStatus;
import com.erp.business.sales.quotation.dto.CreateSalesQuotationRequest;
import com.erp.business.sales.quotation.dto.SalesQuotationResponse;
import com.erp.business.sales.salesorder.dto.SalesOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SalesQuotationService {

    SalesQuotationResponse createQuotation(Long tenantId, Long companyId, CreateSalesQuotationRequest request);

    SalesQuotationResponse getQuotationById(Long tenantId, Long companyId, Long id);

    Page<SalesQuotationResponse> listQuotations(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesQuotationResponse> listQuotationsByStatus(Long tenantId, Long companyId,
                                                        SalesQuotationStatus status, Pageable pageable);

    Page<SalesQuotationResponse> searchQuotations(Long tenantId, Long companyId, String term, Pageable pageable);

    SalesQuotationResponse sendQuotation(Long tenantId, Long companyId, Long id);

    SalesQuotationResponse acceptQuotation(Long tenantId, Long companyId, Long id);

    SalesQuotationResponse rejectQuotation(Long tenantId, Long companyId, Long id, String reason);

    SalesOrderResponse convertToSalesOrder(Long tenantId, Long companyId, Long id);
}
