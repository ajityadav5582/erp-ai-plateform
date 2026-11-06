package com.erp.business.sales.integration;

import com.erp.business.pos.domain.PosTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PosTransactionRepository extends JpaRepository<PosTransaction, Long> {
    Optional<PosTransaction> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
}
