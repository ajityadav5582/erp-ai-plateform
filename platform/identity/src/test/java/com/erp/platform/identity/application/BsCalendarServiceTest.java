package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.MasterBsCalendarResponse;
import com.erp.platform.identity.domain.BsCalendarData;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
    @DisplayName("Convert AD date 2026-04-14 to BS date 2083-01-01 correctly")
    void testAdToBsConversion2083() {
        LocalDate adDate = LocalDate.of(2026, 4, 14);
        BsCalendarData.BsDateDetails details = BsCalendarData.convertAdToBs(adDate);

        assertThat(details.bsYear()).isEqualTo(2083);
        assertThat(details.bsMonth()).isEqualTo(1);
        assertThat(details.bsDay()).isEqualTo(1);
        assertThat(details.bsDateFormatted()).isEqualTo("2083-01-01");
        assertThat(details.monthNameEn()).isEqualTo("Baishakh");
        assertThat(details.monthNameNp()).isEqualTo("वैशाख");
        assertThat(details.dayNameEn()).isEqualTo("Tuesday");
        assertThat(details.dayOfWeek()).isEqualTo(3);
    }

    @Test
    @DisplayName("Convert BS date 2083-01-01 to AD date 2026-04-14 correctly")
    void testBsToAdConversion2083() {
        LocalDate adDate = BsCalendarData.convertBsToAd(2083, 1, 1);
        assertThat(adDate).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("Service getByAdDate returns repository record if present")
    void testGetByAdDateFromRepo() {
        LocalDate adDate = LocalDate.of(2026, 4, 14);
        MasterBsCalendar entity = MasterBsCalendar.create(
                adDate, "2083-01-01", 2083, 1, 1,
                "वैशाख", "Baishakh", 3, "Tuesday", "मङ्गलबार",
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
    @DisplayName("Invalid BS date format throws IllegalArgumentException")
    void testInvalidBsDateFormat() {
        assertThatThrownBy(() -> bsCalendarService.getByBsDate("2083/01/01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid BS date format");
    }
}
