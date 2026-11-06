package com.erp.business.inventory.taxrate;

import com.erp.business.inventory.domain.TaxRate;
import com.erp.business.inventory.domain.TaxabilityType;
import com.erp.business.inventory.infrastructure.persistence.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaxRateServiceImpl implements TaxRateService {

    private final TaxRateRepository repository;

    public TaxRateServiceImpl(TaxRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaxRate create(Long tenantId, Long companyId, String code, String name, BigDecimal rate,
                         TaxabilityType applicability, LocalDate effectiveFrom, LocalDate effectiveTo) {
        TaxRate tr = TaxRate.builder()
                .code(code)
                .name(name)
                .rate(rate)
                .applicability(applicability)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .isActive(Boolean.TRUE)
                .build();
        tr.assignScope(tenantId, companyId);
        return repository.save(tr);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRate> list(Long tenantId, Long companyId) {
        return repository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxRate> findActive(Long tenantId, Long companyId, String code, LocalDate asOf) {
        return repository.findFirstByTenantIdAndCompanyIdAndCodeAndIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                tenantId, companyId, code, asOf);
    }
}
