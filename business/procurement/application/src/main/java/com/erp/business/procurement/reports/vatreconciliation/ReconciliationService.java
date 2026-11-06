package com.erp.business.procurement.reports.vatreconciliation;

public interface ReconciliationService {
    ReconciliationSummary getSummary(Long tenantId, Long companyId, int bsYear, int bsMonth);
}
