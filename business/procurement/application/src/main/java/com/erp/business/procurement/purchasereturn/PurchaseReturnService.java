package com.erp.business.procurement.purchasereturn;

import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.dto.CreatePurchaseReturnRequest;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnResponse;
import com.erp.business.procurement.purchasereturn.dto.ReturnDecisionRequest;
import com.erp.business.procurement.purchasereturn.dto.ReturnableLineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseReturnService {

    PurchaseReturnResponse createReturn(Long tenantId, Long companyId, CreatePurchaseReturnRequest request);

    PurchaseReturnResponse getReturnById(Long tenantId, Long companyId, Long id);

    PurchaseReturnResponse getReturnByDebitNoteNumber(Long tenantId, Long companyId, String debitNoteNumber);

    Page<PurchaseReturnResponse> listReturns(Long tenantId, Long companyId, Pageable pageable);

    Page<PurchaseReturnResponse> listReturnsByStatus(Long tenantId, Long companyId, PurchaseReturnStatus status, Pageable pageable);

    Page<PurchaseReturnResponse> listReturnsByInvoice(Long tenantId, Long companyId, Long invoiceId, Pageable pageable);

    Page<PurchaseReturnResponse> searchReturns(Long tenantId, Long companyId, String term, Pageable pageable);

    PurchaseReturnResponse approveReturn(Long tenantId, Long companyId, Long id);

    PurchaseReturnResponse postReturn(Long tenantId, Long companyId, Long id);

    PurchaseReturnResponse cancelReturn(Long tenantId, Long companyId, Long id, ReturnDecisionRequest request);

    PurchaseReturnResponse retryStockPost(Long tenantId, Long companyId, Long id);

    List<ReturnableLineResponse> getReturnableLinesForInvoice(Long tenantId, Long companyId, Long invoiceId);
}

