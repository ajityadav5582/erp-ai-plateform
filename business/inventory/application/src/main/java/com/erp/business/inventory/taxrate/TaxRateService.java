package com.erp.business.inventory.taxrate;

import com.erp.business.inventory.domain.TaxRate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxRateService {
    TaxRate create(Long tenantId, Long companyId, String code, String name, java.math.BigDecimal rate,
                   com.erp.business.inventory.domain.TaxabilityType applicability,
                   LocalDate effectiveFrom, LocalDate effectiveTo);

    List<TaxRate> list(Long tenantId, Long companyId);

    Optional<TaxRate> findActive(Long tenantId, Long companyId, String code, LocalDate asOf);
}
