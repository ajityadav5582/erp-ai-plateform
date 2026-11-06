package com.erpai.integration.ird.infrastructure.persistence;

import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.port.out.CompanyIrdConfigRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link CompanyIrdConfigRepositoryPort}.
 *
 * <p>Registered explicitly via {@code @EnableJpaRepositories} in the host
 * application(s) (sales/pos), because the integration module sits outside their
 * component-scan root. Without this registration the entity has no table and the
 * repository bean does not exist.
 */
public interface CompanyIrdConfigRepository
        extends JpaRepository<CompanyIrdConfig, Long>, CompanyIrdConfigRepositoryPort {

    @Override
    Optional<CompanyIrdConfig> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    @Override
    void deleteByTenantIdAndCompanyId(Long tenantId, Long companyId);
}
