package com.erp.business.pos.poscompanyconfig.infrastructure.persistence;

import com.erp.business.pos.domain.PosCompanyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence port for {@link PosCompanyConfig}. One row per (tenant, company).
 */
public interface PosCompanyConfigRepository extends JpaRepository<PosCompanyConfig, Long> {

    Optional<PosCompanyConfig> findByTenantIdAndCompanyId(Long tenantId, Long companyId);
}
