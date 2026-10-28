package com.erp.business.pos.kitchen;

import com.erp.business.pos.kitchen.dto.PosKitchenTicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PosKitchenTicketService {
    PosKitchenTicketResponse createTicket(Long tenantId, Long companyId, Long posTransactionId);
    PosKitchenTicketResponse getTicketById(Long tenantId, Long companyId, Long id);
    List<PosKitchenTicketResponse> listByTransaction(Long tenantId, Long companyId, Long posTransactionId);
    List<PosKitchenTicketResponse> listByStatus(Long tenantId, Long companyId, String status);
    PosKitchenTicketResponse markPreparing(Long tenantId, Long companyId, Long id);
    PosKitchenTicketResponse markReady(Long tenantId, Long companyId, Long id);
    PosKitchenTicketResponse markServed(Long tenantId, Long companyId, Long id);
    PosKitchenTicketResponse cancel(Long tenantId, Long companyId, Long id);
}
