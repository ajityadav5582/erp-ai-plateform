package com.erp.platform.identity.interfaces.rest;

import com.erp.platform.identity.application.BsCalendarService;
import com.erp.platform.identity.application.dto.MasterBsCalendarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/master-bs-calendar")
@RequiredArgsConstructor
public class MasterBsCalendarController {

    private final BsCalendarService bsCalendarService;

    @GetMapping("/ad-to-bs")
    public MasterBsCalendarResponse getByAdDate(
            @RequestParam("adDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate adDate) {
        return bsCalendarService.getByAdDate(adDate);
    }

    @GetMapping("/bs-to-ad")
    public MasterBsCalendarResponse getByBsDate(
            @RequestParam("bsDate") String bsDate) {
        return bsCalendarService.getByBsDate(bsDate);
    }

    @GetMapping("/month")
    public List<MasterBsCalendarResponse> getByMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        return bsCalendarService.getByMonth(year, month);
    }

    @GetMapping("/fiscal-year/{fiscalYearId}")
    public List<MasterBsCalendarResponse> getByMasterFiscalYear(
            @PathVariable("fiscalYearId") Short fiscalYearId) {
        return bsCalendarService.getByMasterFiscalYear(fiscalYearId);
    }

    @GetMapping("/range")
    public List<MasterBsCalendarResponse> getByAdDateRange(
            @RequestParam("startAdDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAdDate,
            @RequestParam("endAdDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endAdDate) {
        return bsCalendarService.getByAdDateRange(startAdDate, endAdDate);
    }
}
