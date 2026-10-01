package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.MasterBsCalendarResponse;
import com.erp.platform.identity.domain.BsCalendarData;
import com.erp.platform.identity.domain.MasterBsCalendar;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.infrastructure.persistence.MasterBsCalendarRepository;
import com.erp.platform.identity.infrastructure.persistence.MasterFiscalYearRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BsCalendarService {

    private static final Logger log = LoggerFactory.getLogger(BsCalendarService.class);

    private final MasterBsCalendarRepository calendarRepository;
    private final MasterFiscalYearRepository fiscalYearRepository;

    @Transactional(readOnly = true)
    public MasterBsCalendarResponse getByAdDate(LocalDate adDate) {
        Optional<MasterBsCalendar> found = calendarRepository.findByAdDate(adDate);
        if (found.isPresent()) {
            return MasterBsCalendarResponse.from(found.get());
        }
        // Fallback to calculation
        BsCalendarData.BsDateDetails details = BsCalendarData.convertAdToBs(adDate);
        MasterFiscalYear matchingFy = findMatchingFiscalYear(adDate);
        MasterBsCalendar transientEntity = MasterBsCalendar.create(
                adDate, details.bsDateFormatted(), details.bsYear(), details.bsMonth(), details.bsDay(),
                details.monthNameNp(), details.monthNameEn(), details.dayOfWeek(), details.dayNameEn(),
                details.dayNameNp(), details.isHoliday(), matchingFy);
        return MasterBsCalendarResponse.from(transientEntity);
    }

    @Transactional(readOnly = true)
    public MasterBsCalendarResponse getByBsDate(String bsDate) {
        Optional<MasterBsCalendar> found = calendarRepository.findByBsDate(bsDate);
        if (found.isPresent()) {
            return MasterBsCalendarResponse.from(found.get());
        }
        // Parse BS date YYYY-MM-DD
        String[] parts = bsDate.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid BS date format: " + bsDate + ". Expected format: YYYY-MM-DD");
        }
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        LocalDate adDate = BsCalendarData.convertBsToAd(year, month, day);
        return getByAdDate(adDate);
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByMonth(int bsYear, int bsMonth) {
        List<MasterBsCalendar> list = calendarRepository.findByBsYearAndBsMonthOrderByAdDateAsc(bsYear, bsMonth);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        // Fallback: calculate days in month
        if (bsYear < BsCalendarData.START_YEAR || bsYear > BsCalendarData.END_YEAR || bsMonth < 1 || bsMonth > 12) {
            throw new IllegalArgumentException("Unsupported BS Year/Month: " + bsYear + "-" + bsMonth);
        }
        int maxDays = BsCalendarData.MONTH_DAYS[bsYear - BsCalendarData.START_YEAR][bsMonth - 1];
        List<MasterBsCalendarResponse> responses = new ArrayList<>(maxDays);
        for (int d = 1; d <= maxDays; d++) {
            String bsDate = String.format("%04d-%02d-%02d", bsYear, bsMonth, d);
            responses.add(getByBsDate(bsDate));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByMasterFiscalYear(Short masterFiscalYearId) {
        List<MasterBsCalendar> list = calendarRepository.findByMasterFiscalYearIdOrderByAdDateAsc(masterFiscalYearId);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        MasterFiscalYear fy = fiscalYearRepository.findById(masterFiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("Master fiscal year not found: " + masterFiscalYearId));
        return getByAdDateRange(fy.getStartDateAd(), fy.getEndDateAd());
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByAdDateRange(LocalDate startDate, LocalDate endDate) {
        List<MasterBsCalendar> list = calendarRepository.findByAdDateBetweenOrderByAdDateAsc(startDate, endDate);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        List<MasterBsCalendarResponse> responses = new ArrayList<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            responses.add(getByAdDate(curr));
            curr = curr.plusDays(1);
        }
        return responses;
    }

    @Transactional
    public void seedBsCalendarRange(int startBsYear, int endBsYear) {
        log.info("Seeding Master BS Calendar for years {} to {}", startBsYear, endBsYear);
        List<MasterFiscalYear> fiscalYears = fiscalYearRepository.findAll();

        List<MasterBsCalendar> batch = new ArrayList<>();
        for (int y = startBsYear; y <= endBsYear; y++) {
            if (y < BsCalendarData.START_YEAR || y > BsCalendarData.END_YEAR) {
                continue;
            }
            int[] months = BsCalendarData.MONTH_DAYS[y - BsCalendarData.START_YEAR];
            for (int m = 1; m <= 12; m++) {
                int daysInMonth = months[m - 1];
                for (int d = 1; d <= daysInMonth; d++) {
                    LocalDate adDate = BsCalendarData.convertBsToAd(y, m, d);
                    if (calendarRepository.existsByAdDate(adDate)) {
                        continue;
                    }
                    String bsDateFormatted = String.format("%04d-%02d-%02d", y, m, d);
                    int dayOfWeek = adDate.getDayOfWeek().getValue() % 7 + 1;
                    boolean isHoliday = (dayOfWeek == 7);

                    MasterFiscalYear matchingFy = fiscalYears.stream()
                            .filter(fy -> !adDate.isBefore(fy.getStartDateAd()) && !adDate.isAfter(fy.getEndDateAd()))
                            .findFirst()
                            .orElse(null);

                    MasterBsCalendar calendar = MasterBsCalendar.create(
                            adDate, bsDateFormatted, y, m, d,
                            BsCalendarData.MONTH_NAMES_NP[m], BsCalendarData.MONTH_NAMES_EN[m],
                            dayOfWeek, BsCalendarData.DAY_NAMES_EN[dayOfWeek], BsCalendarData.DAY_NAMES_NP[dayOfWeek],
                            isHoliday, matchingFy
                    );
                    batch.add(calendar);
                }
            }
        }

        if (!batch.isEmpty()) {
            calendarRepository.saveAll(batch);
            log.info("Successfully seeded {} Master BS Calendar dates", batch.size());
        }
    }

    @Transactional
    public void relinkFiscalYears() {
        List<MasterFiscalYear> fiscalYears = fiscalYearRepository.findAll();
        for (MasterFiscalYear fy : fiscalYears) {
            List<MasterBsCalendar> dates = calendarRepository.findByAdDateBetweenOrderByAdDateAsc(fy.getStartDateAd(), fy.getEndDateAd());
            for (MasterBsCalendar date : dates) {
                if (date.getMasterFiscalYear() == null || !date.getMasterFiscalYear().getId().equals(fy.getId())) {
                    date.setMasterFiscalYear(fy);
                }
            }
            calendarRepository.saveAll(dates);
        }
    }

    private MasterFiscalYear findMatchingFiscalYear(LocalDate adDate) {
        return fiscalYearRepository.findAll().stream()
                .filter(fy -> !adDate.isBefore(fy.getStartDateAd()) && !adDate.isAfter(fy.getEndDateAd()))
                .findFirst()
                .orElse(null);
    }
}
