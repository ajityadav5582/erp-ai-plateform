package com.erp.business.inventory.interfaces.tax;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TaxRateService {
    record TaxRate(Long id, String code, String name, BigDecimal rate,
                   String applicability, LocalDate effectiveFrom, LocalDate effectiveTo) {}

    TaxRate findActive(Long tenantId, Long companyId, String code, LocalDate asOf);
}
