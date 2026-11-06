package com.erp.business.reports.vat.annex10;

public interface VatReturnService {
    VatReturnResponse compute(Long tenantId, Long companyId, Long fiscalYearId, int nepaliMonth);
    byte[] exportPdf(Long tenantId, Long companyId, Long fiscalYearId, int nepaliMonth);
}
