package com.erp.platform.identity.application.dto;

import com.erp.platform.identity.domain.MasterBsCalendar;

import java.time.LocalDate;

public record MasterBsCalendarResponse(
        Long id,
        LocalDate adDate,
        String bsDate,
        Integer bsYear,
        Integer bsMonth,
        Integer bsDay,
        String bsMonthNameNp,
        String bsMonthNameEn,
        Integer dayOfWeek,
        String dayNameEn,
        String dayNameNp,
        Boolean isHoliday,
        Short masterFiscalYearId,
        String masterFiscalYearCode
) {
    public static MasterBsCalendarResponse from(MasterBsCalendar calendar) {
        Short fyId = calendar.getMasterFiscalYear() != null ? calendar.getMasterFiscalYear().getId() : null;
        String fyCode = calendar.getMasterFiscalYear() != null ? calendar.getMasterFiscalYear().getCode() : null;
        return new MasterBsCalendarResponse(
                calendar.getId(),
                calendar.getAdDate(),
                calendar.getBsDate(),
                calendar.getBsYear(),
                calendar.getBsMonth(),
                calendar.getBsDay(),
                calendar.getBsMonthNameNp(),
                calendar.getBsMonthNameEn(),
                calendar.getDayOfWeek(),
                calendar.getDayNameEn(),
                calendar.getDayNameNp(),
                calendar.getIsHoliday(),
                fyId,
                fyCode
        );
    }
}
