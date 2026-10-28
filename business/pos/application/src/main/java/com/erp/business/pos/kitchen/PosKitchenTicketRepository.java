package com.erp.business.pos.kitchen;

import com.erp.business.pos.domain.PosKitchenTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosKitchenTicketRepository extends JpaRepository<PosKitchenTicket, Long> {
    Optional<PosKitchenTicket> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
    List<PosKitchenTicket> findByTenantIdAndCompanyIdAndPosTransactionId(Long tenantId, Long companyId, Long posTransactionId);
    List<PosKitchenTicket> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId, String status);
}
