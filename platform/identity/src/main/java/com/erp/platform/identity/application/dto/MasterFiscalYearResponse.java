package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.MasterFiscalYear;

public record MasterFiscalYearResponse(
        Short id,
        String name,
        String code,
        String startDateBs,
        String endDateBs,
        String startDateAd,
        String endDateAd,
        Short displayOrder
) {
    public static MasterFiscalYearResponse from(MasterFiscalYear year) {
        return new MasterFiscalYearResponse(year.getId(), year.getName(), year.getCode(),
                year.getStartDateBs(), year.getEndDateBs(), year.getStartDateAd().toString(),
                year.getEndDateAd().toString(), year.getDisplayOrder());
    }
}
