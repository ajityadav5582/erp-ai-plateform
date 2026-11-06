package com.erp.business.inventory.interfaces.adapter;

import com.erp.business.inventory.domain.TaxRate;
import com.erp.business.inventory.infrastructure.persistence.TaxRateRepository;
import com.erp.business.inventory.interfaces.tax.TaxRateService;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class TaxRateServiceAdapter implements TaxRateService {

    private final TaxRateRepository repository;

    public TaxRateServiceAdapter(TaxRateRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaxRateService.TaxRate findActive(Long tenantId, Long companyId, String code, LocalDate asOf) {
        return repository.findFirstByTenantIdAndCompanyIdAndCodeAndIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                tenantId, companyId, code, asOf)
                .map(tr -> new TaxRateService.TaxRate(
                        tr.getId(), tr.getCode(), tr.getName(), tr.getRate(),
                        tr.getApplicability().name(), tr.getEffectiveFrom(), tr.getEffectiveTo()))
                .orElse(null);
    }
}
