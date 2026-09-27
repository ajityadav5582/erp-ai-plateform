package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.CompanyFiscalYear;
import com.erp.platform.identity.domain.FiscalYearType;

public record CompanyFiscalYearResponse(
        Long id,
        Long companyId,
        Short masterFiscalYearId,
        String name,
        String code,
        String startDateBs,
        String endDateBs,
        String startDateAd,
        String endDateAd,
        FiscalYearType calendarType,
        boolean active
) {
    public static CompanyFiscalYearResponse from(CompanyFiscalYear year) {
        return new CompanyFiscalYearResponse(year.getId(), year.getCompanyId(), year.getMasterFiscalYearId(),
                year.getName(), year.getCode(), year.getStartDateBs(), year.getEndDateBs(),
                year.getStartDateAd() == null ? null : year.getStartDateAd().toString(),
                year.getEndDateAd() == null ? null : year.getEndDateAd().toString(),
                year.getCalendarType(), year.isActive());
    }
}
