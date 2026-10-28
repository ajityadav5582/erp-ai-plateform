package com.erp.platform.identity.application;

import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.common.nepali.date.NepaliDateDetails;
import com.erp.platform.common.nepali.date.NepaliDateException;
import com.erp.platform.identity.application.dto.MasterBsCalendarResponse;
import com.erp.platform.identity.domain.MasterBsCalendar;
import com.erp.platform.identity.domain.MasterFiscalYear;
import com.erp.platform.identity.infrastructure.persistence.MasterBsCalendarRepository;
import com.erp.platform.identity.infrastructure.persistence.MasterFiscalYearRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies that the calendar service delegates to the shared
 * {@code libraries/common-nepali-date} library and returns real Bikram Sambat dates.
 *
 * <p>The previous version of this test asserted "Baishakh" and "वैशाख" and relied on the
 * fabricated in-identity month table. Both are gone.
 */
@ExtendWith(MockitoExtension.class)
class BsCalendarServiceTest {

    @Mock
    private MasterBsCalendarRepository calendarRepository;

    @Mock
    private MasterFiscalYearRepository fiscalYearRepository;

    @InjectMocks
    private BsCalendarService bsCalendarService;

    private MasterFiscalYear mockFy2083;

    @BeforeEach
    void setUp() {
        mockFy2083 = MasterFiscalYear.create(
                "FY 2083/84",
                "2083-84",
                "2083-04-01",
                "2084-03-31",
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2027, 7, 16),
                (short) 6
        );
    }

    @Test
    @DisplayName("2026-04-14 is Baisakh 1, 2083 BS (Nepali New Year)")
    void testAdToBsConversion2083() {
        LocalDate adDate = LocalDate.of(2026, 4, 14);
        NepaliDateDetails details = NepaliDateConverter.details(adDate);

        assertThat(details.bsYear()).isEqualTo(2083);
        assertThat(details.bsMonth()).isEqualTo(1);
        assertThat(details.bsDay()).isEqualTo(1);
        assertThat(details.bsDate()).isEqualTo("2083-01-01");
        assertThat(details.monthNameEn()).isEqualTo("Baisakh");
        assertThat(details.monthNameNp()).isEqualTo("बैशाख");
        assertThat(details.dayNameEn()).isEqualTo("Tuesday");
        assertThat(details.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @Test
    @DisplayName("BS 2083-01-01 converts back to AD 2026-04-14")
    void testBsToAdConversion2083() {
        assertThat(NepaliDateConverter.toAd(2083, 1, 1)).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("Regression: 2024-04-13 is Baisakh 1, 2081 BS (the old table said 2081-01-02)")
    void testKnownNepaliNewYear2081() {
        NepaliDateDetails details = NepaliDateConverter.details(LocalDate.of(2024, 4, 13));
        assertThat(details.bsDate()).isEqualTo("2081-01-01");
    }

    @Test
    @DisplayName("Service getByAdDate returns repository record if present")
    void testGetByAdDateFromRepo() {
        LocalDate adDate = LocalDate.of(2026, 4, 14);
        MasterBsCalendar entity = MasterBsCalendar.create(
                adDate, "2083-01-01", 2083, 1, 1,
                "बैशाख", "Baisakh", 2, "Tuesday", "मङ्गलबार",
                false, mockFy2083
        );
        when(calendarRepository.findByAdDate(adDate)).thenReturn(Optional.of(entity));

        MasterBsCalendarResponse response = bsCalendarService.getByAdDate(adDate);

        assertThat(response.bsDate()).isEqualTo("2083-01-01");
        assertThat(response.adDate()).isEqualTo(adDate);
        assertThat(response.masterFiscalYearCode()).isEqualTo("2083-84");
    }

    @Test
    @DisplayName("Service getByBsDate fallback converts string and matches fiscal year")
    void testGetByBsDateFallback() {
        String bsDate = "2083-04-01";
        LocalDate expectedAdDate = LocalDate.of(2026, 7, 17);

        when(calendarRepository.findByBsDate(bsDate)).thenReturn(Optional.empty());
        when(calendarRepository.findByAdDate(expectedAdDate)).thenReturn(Optional.empty());
        when(fiscalYearRepository.findAll()).thenReturn(List.of(mockFy2083));

        MasterBsCalendarResponse response = bsCalendarService.getByBsDate(bsDate);

        assertThat(response.adDate()).isEqualTo(expectedAdDate);
        assertThat(response.bsDate()).isEqualTo("2083-04-01");
        assertThat(response.masterFiscalYearCode()).isEqualTo("2083-84");
    }

    @Test
    @DisplayName("Malformed BS date is rejected with a clear message")
    void testInvalidBsDateFormat() {
        assertThatThrownBy(() -> bsCalendarService.getByBsDate("not-a-date"))
                .isInstanceOf(NepaliDateException.class)
                .hasMessageContaining("Malformed BS date");
    }

    @Test
    @DisplayName("Out-of-range BS date is rejected")
    void testOutOfRangeBsDate() {
        assertThatThrownBy(() -> bsCalendarService.getByBsDate("1900-01-01"))
                .isInstanceOf(NepaliDateException.class);
    }
}
