package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.CompanyFiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyFiscalYearRepository extends JpaRepository<CompanyFiscalYear, Long> {
    List<CompanyFiscalYear> findByTenantIdAndCompanyIdOrderByStartDateDesc(Long tenantId, Long companyId);
    Optional<CompanyFiscalYear> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
    boolean existsByTenantIdAndCompanyIdAndCodeIgnoreCase(Long tenantId, Long companyId, String code);
}
