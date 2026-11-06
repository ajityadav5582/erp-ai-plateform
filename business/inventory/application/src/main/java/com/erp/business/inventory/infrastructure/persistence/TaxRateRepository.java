package com.erp.business.inventory.infrastructure.persistence;

import com.erp.business.inventory.domain.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    List<TaxRate> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    Optional<TaxRate> findFirstByTenantIdAndCompanyIdAndCodeAndIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long tenantId, Long companyId, String code, LocalDate asOf);
}
