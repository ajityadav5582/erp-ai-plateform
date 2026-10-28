package com.erp.platform.identity.application;

import com.erp.platform.common.nepali.date.BsCalendarData;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.common.nepali.date.NepaliDateDetails;
import com.erp.platform.identity.application.dto.MasterBsCalendarResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serves Bikram Sambat calendar data.
 *
 * <p>All calendar arithmetic is delegated to {@code libraries/common-nepali-date}. This
 * class no longer holds any month-length data of its own; it only reads persisted rows
 * and, when a date is missing from the table, computes it with the shared converter.
 */
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
        // Not persisted yet: compute it with the shared library.
        NepaliDateDetails details = NepaliDateConverter.details(adDate);
        return MasterBsCalendarResponse.from(toTransient(adDate, details));
    }

    @Transactional(readOnly = true)
    public MasterBsCalendarResponse getByBsDate(String bsDate) {
        Optional<MasterBsCalendar> found = calendarRepository.findByBsDate(bsDate);
        if (found.isPresent()) {
            return MasterBsCalendarResponse.from(found.get());
        }
        // Parse and validate via the library, which rejects malformed or out-of-range input.
        return getByAdDate(NepaliDateConverter.toAd(bsDate));
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByMonth(int bsYear, int bsMonth) {
        List<MasterBsCalendar> list =
                calendarRepository.findByBsYearAndBsMonthOrderByAdDateAsc(bsYear, bsMonth);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        int maxDays = BsCalendarData.daysInMonth(bsYear, bsMonth);
        List<MasterBsCalendarResponse> responses = new ArrayList<>(maxDays);
        for (int d = 1; d <= maxDays; d++) {
            responses.add(getByAdDate(NepaliDateConverter.toAd(bsYear, bsMonth, d)));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByMasterFiscalYear(Short masterFiscalYearId) {
        List<MasterBsCalendar> list =
                calendarRepository.findByMasterFiscalYearIdOrderByAdDateAsc(masterFiscalYearId);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        MasterFiscalYear fy = fiscalYearRepository.findById(masterFiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("Master fiscal year not found: " + masterFiscalYearId));
        return getByAdDateRange(fy.getStartDateAd(), fy.getEndDateAd());
    }

    @Transactional(readOnly = true)
    public List<MasterBsCalendarResponse> getByAdDateRange(LocalDate startDate, LocalDate endDate) {
        List<MasterBsCalendar> list =
                calendarRepository.findByAdDateBetweenOrderByAdDateAsc(startDate, endDate);
        if (!list.isEmpty()) {
            return list.stream().map(MasterBsCalendarResponse::from).toList();
        }
        List<MasterBsCalendarResponse> responses = new ArrayList<>();
        for (LocalDate curr = startDate; !curr.isAfter(endDate); curr = curr.plusDays(1)) {
            responses.add(getByAdDate(curr));
        }
        return responses;
    }

    /**
     * Persists calendar rows for the given BS years using the shared converter.
     *
     * <p>This replaces the previous hand-rolled loop that read the fabricated month table
     * and computed the weekday as {@code getDayOfWeek().getValue() % 7 + 1}, which produced
     * a Sunday-first index that did not match the Monday-first name arrays.
     */
    @Transactional
    public void seedBsCalendarRange(int startBsYear, int endBsYear) {
        log.info("Seeding Master BS Calendar for BS years {} to {}", startBsYear, endBsYear);
        List<MasterFiscalYear> fiscalYears = fiscalYearRepository.findAll();

        // Collect all AD dates and their details first
        List<LocalDate> allAdDates = new ArrayList<>();
        List<NepaliDateDetails> allDetails = new ArrayList<>();
        for (int y = startBsYear; y <= endBsYear; y++) {
            if (!BsCalendarData.isSupportedYear(y)) {
                continue;
            }
            for (NepaliDateDetails details : NepaliDateConverter.expandBsYear(y)) {
                allAdDates.add(details.adDate());
                allDetails.add(details);
            }
        }

        if (allAdDates.isEmpty()) {
            return;
        }

        // Query all existing dates in the range in a single query instead of
        // N+1 individual existsByAdDate calls.
        LocalDate minDate = Collections.min(allAdDates);
        LocalDate maxDate = Collections.max(allAdDates);
        List<MasterBsCalendar> existing = calendarRepository.findByAdDateBetweenOrderByAdDateAsc(minDate, maxDate);
        Set<LocalDate> existingDates = existing.stream()
                .map(MasterBsCalendar::getAdDate)
                .collect(Collectors.toSet());

        List<MasterBsCalendar> batch = new ArrayList<>();
        for (int i = 0; i < allAdDates.size(); i++) {
            LocalDate adDate = allAdDates.get(i);
            if (existingDates.contains(adDate)) {
                continue;
            }
            NepaliDateDetails details = allDetails.get(i);
            batch.add(MasterBsCalendar.create(
                    adDate,
                    details.bsDate(),
                    details.bsYear(),
                    details.bsMonth(),
                    details.bsDay(),
                    details.monthNameNp(),
                    details.monthNameEn(),
                    details.dayOfWeek().getValue(),
                    details.dayNameEn(),
                    details.dayNameNp(),
                    details.weekend(),
                    findMatchingFiscalYear(adDate, fiscalYears)));
        }

        if (!batch.isEmpty()) {
            calendarRepository.saveAll(batch);
            log.info("Successfully seeded {} Master BS Calendar dates", batch.size());
        }
    }

    /** Re-points every calendar day at the fiscal year that actually contains it. */
    @Transactional
    public void relinkFiscalYears() {
        List<MasterFiscalYear> fiscalYears = fiscalYearRepository.findAll();
        for (MasterFiscalYear fy : fiscalYears) {
            List<MasterBsCalendar> dates =
                    calendarRepository.findByAdDateBetweenOrderByAdDateAsc(fy.getStartDateAd(), fy.getEndDateAd());
            for (MasterBsCalendar date : dates) {
                if (date.getMasterFiscalYear() == null
                        || !date.getMasterFiscalYear().getId().equals(fy.getId())) {
                    date.setMasterFiscalYear(fy);
                }
            }
            calendarRepository.saveAll(dates);
        }
    }

    private MasterBsCalendar toTransient(LocalDate adDate, NepaliDateDetails details) {
        return MasterBsCalendar.create(
                adDate,
                details.bsDate(),
                details.bsYear(),
                details.bsMonth(),
                details.bsDay(),
                details.monthNameNp(),
                details.monthNameEn(),
                details.dayOfWeek().getValue(),
                details.dayNameEn(),
                details.dayNameNp(),
                details.weekend(),
                findMatchingFiscalYear(adDate, fiscalYearRepository.findAll()));
    }

    private MasterFiscalYear findMatchingFiscalYear(LocalDate adDate, List<MasterFiscalYear> fiscalYears) {
        return fiscalYears.stream()
                .filter(fy -> !adDate.isBefore(fy.getStartDateAd()) && !adDate.isAfter(fy.getEndDateAd()))
                .findFirst()
                .orElse(null);
    }
}
