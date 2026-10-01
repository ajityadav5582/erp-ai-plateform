package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.MasterBsCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MasterBsCalendarRepository extends JpaRepository<MasterBsCalendar, Long> {

    Optional<MasterBsCalendar> findByAdDate(LocalDate adDate);

    Optional<MasterBsCalendar> findByBsDate(String bsDate);

    List<MasterBsCalendar> findByBsYearAndBsMonthOrderByAdDateAsc(Integer bsYear, Integer bsMonth);

    List<MasterBsCalendar> findByMasterFiscalYearIdOrderByAdDateAsc(Short masterFiscalYearId);

    List<MasterBsCalendar> findByAdDateBetweenOrderByAdDateAsc(LocalDate startDate, LocalDate endDate);

    List<MasterBsCalendar> findByBsDateBetweenOrderByAdDateAsc(String startBsDate, String endBsDate);

    boolean existsByAdDate(LocalDate adDate);

    @Query("SELECT MIN(c.adDate) FROM MasterBsCalendar c")
    Optional<LocalDate> findMinAdDate();

    @Query("SELECT MAX(c.adDate) FROM MasterBsCalendar c")
    Optional<LocalDate> findMaxAdDate();
}
