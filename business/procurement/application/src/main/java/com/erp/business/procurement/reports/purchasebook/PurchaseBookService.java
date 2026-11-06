package com.erp.business.procurement.reports.purchasebook;

import org.springframework.data.domain.Pageable;

public interface PurchaseBookService {

    PurchaseBookResponse getPurchaseBook(Long tenantId, Long companyId, Long fiscalYearId,
                                        String startDateNp, String endDateNp, Long supplierId,
                                        Pageable pageable);

    byte[] exportExcel(Long tenantId, Long companyId, Long fiscalYearId,
                       String startDateNp, String endDateNp, Long supplierId);

    String exportCsv(Long tenantId, Long companyId, Long fiscalYearId,
                     String startDateNp, String endDateNp, Long supplierId);
}
