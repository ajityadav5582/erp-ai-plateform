package com.erp.business.pos.kitchen;

import com.erp.business.pos.domain.PosKitchenTicket;
import com.erp.business.pos.kitchen.dto.PosKitchenTicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PosKitchenTicketServiceImpl implements PosKitchenTicketService {

    private final PosKitchenTicketRepository repository;

    public PosKitchenTicketServiceImpl(PosKitchenTicketRepository repository) {
        this.repository = repository;
    }

    @Override
    public PosKitchenTicketResponse createTicket(Long tenantId, Long companyId, Long posTransactionId) {
        PosKitchenTicket ticket = PosKitchenTicket.builder()
                .posTransactionId(posTransactionId)
                .status("PENDING")
                .build();
        ticket.assignScope(tenantId, companyId);
        return toResponse(repository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public PosKitchenTicketResponse getTicketById(Long tenantId, Long companyId, Long id) {
        PosKitchenTicket ticket = repository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosKitchenTicketResponse> listByTransaction(Long tenantId, Long companyId, Long posTransactionId) {
        return repository.findByTenantIdAndCompanyIdAndPosTransactionId(tenantId, companyId, posTransactionId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosKitchenTicketResponse> listByStatus(Long tenantId, Long companyId, String status) {
        return repository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public PosKitchenTicketResponse markPreparing(Long tenantId, Long companyId, Long id) {
        PosKitchenTicket ticket = load(tenantId, companyId, id);
        ticket.markPreparing();
        return toResponse(repository.save(ticket));
    }

    @Override
    public PosKitchenTicketResponse markReady(Long tenantId, Long companyId, Long id) {
        PosKitchenTicket ticket = load(tenantId, companyId, id);
        ticket.markReady();
        return toResponse(repository.save(ticket));
    }

    @Override
    public PosKitchenTicketResponse markServed(Long tenantId, Long companyId, Long id) {
        PosKitchenTicket ticket = load(tenantId, companyId, id);
        ticket.markServed();
        return toResponse(repository.save(ticket));
    }

    @Override
    public PosKitchenTicketResponse cancel(Long tenantId, Long companyId, Long id) {
        PosKitchenTicket ticket = load(tenantId, companyId, id);
        ticket.cancel();
        return toResponse(repository.save(ticket));
    }

    private PosKitchenTicket load(Long tenantId, Long companyId, Long id) {
        return repository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }

    private PosKitchenTicketResponse toResponse(PosKitchenTicket ticket) {
        return new PosKitchenTicketResponse(ticket.getId(), ticket.getPosTransactionId(), ticket.getStatus());
    }
}
