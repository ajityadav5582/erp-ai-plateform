package com.erp.business.procurement.purchaserequisition.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;

/**
 * Pins down the one invariant the derived Bikram Sambat columns exist to protect:
 * {@code request_date_np} always agrees with {@code request_date}.
 *
 * <p>The entity has no public mutator for the NP columns, and the sync callback runs on
 * every insert and update, so there is no code path - service, seeder, import job or
 * ad-hoc console - that can persist a disagreement. These tests call the callback
 * directly, which is exactly what Hibernate does, so they cover the real code path
 * without needing a database.
 */
class PurchaseRequisitionNepaliDateTest {

    /** Builds a draft with no lines; the projection does not depend on lines. */
    private static PurchaseRequisition draftWith(LocalDate requestDate, LocalDate requiredBy) {
        return PurchaseRequisition.draft(
                "PR-TEST-0001",
                1L,          // branchId
                1L,          // requestedBy
                requestDate,
                requiredBy,
                RequisitionPriority.NORMAL,
                "NPR",
                null,        // fiscalYearId
                null);       // departmentId
    }

    /** Runs the @PrePersist/@PreUpdate callback the way Hibernate would. */
    private static PurchaseRequisition synced(LocalDate requestDate, LocalDate requiredBy) {
        PurchaseRequisition requisition = draftWith(requestDate, requiredBy);
        requisition.syncNepaliDate();
        return requisition;
    }

    @Test
    @DisplayName("projects the request date into BS on a known anchor")
    void projectsKnownAnchor() {
        // 2026-10-02 is 2083 Ashwin 16 in Bikram Sambat.
        PurchaseRequisition requisition = synced(LocalDate.of(2026, 10, 2), null);

        assertEquals("2083-06-16", requisition.getRequestDateNp());
        assertEquals(2083, requisition.getRequestDateNpYear());
    }

    @Test
    @DisplayName("projects the fiscal-year boundary anchor 2081-01-01")
    void projectsFiscalYearBoundary() {
        // Bikram Sambat new year: 2081 Baisakh 1 is 2024-04-13.
        PurchaseRequisition requisition = synced(LocalDate.of(2024, 4, 13), null);

        assertEquals("2081-01-01", requisition.getRequestDateNp());
        assertEquals(2081, requisition.getRequestDateNpYear());
    }

    @Test
    @DisplayName("projects the required-by date independently of the request date")
    void projectsRequiredByDate() {
        PurchaseRequisition requisition =
                synced(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 11, 15));

        assertNotNull(requisition.getRequiredByDateNp());
        assertEquals(NepaliDateConverter.fromAd(LocalDate.of(2026, 11, 15)).toString(),
                requisition.getRequiredByDateNp());
    }

    @Test
    @DisplayName("leaves the required-by projection null when that date is absent")
    void requiredByProjectionIsNullWhenDateAbsent() {
        PurchaseRequisition requisition = synced(LocalDate.of(2026, 10, 2), null);

        assertNull(requisition.getRequiredByDateNp());
    }

    @Test
    @DisplayName("re-syncs after the AD date changes, so a stale projection cannot survive")
    void resyncsWhenRequestDateChanges() {
        PurchaseRequisition requisition = synced(LocalDate.of(2026, 10, 2), null);
        assertEquals("2083-06-16", requisition.getRequestDateNp());

        // The builder is the only way to move the AD date; then re-run the callback.
        requisition = PurchaseRequisition.builder()
                .prNumber("PR-TEST-0001")
                .requestDate(LocalDate.of(2024, 4, 13))
                .requestDateNp("2083-06-16")   // deliberately stale
                .requestDateNpYear(2083)
                .build();
        requisition.syncNepaliDate();

        assertEquals("2081-01-01", requisition.getRequestDateNp());
        assertEquals(2081, requisition.getRequestDateNpYear());
    }

    @Test
    @DisplayName("stores a null projection for a date outside the published calendar range")
    void nullProjectionOutsideCalendarRange() {
        // 1850 is far below the 2000 BS start of the calendar table.
        PurchaseRequisition requisition = synced(LocalDate.of(1850, 1, 1), null);

        assertNull(requisition.getRequestDateNp());
        assertNull(requisition.getRequestDateNpYear());
        // The real business data is still recorded.
        assertEquals(LocalDate.of(1850, 1, 1), requisition.getRequestDate());
    }

    @Test
    @DisplayName("stays in agreement with the converter across a full year of dates")
    void agreesWithConverterAcrossAYear() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        while (!date.isAfter(end)) {
            PurchaseRequisition requisition = synced(date, null);
            NepaliDate expected = NepaliDateConverter.fromAd(date);

            assertEquals(expected.toString(), requisition.getRequestDateNp(),
                    "BS projection drifted for " + date);
            assertEquals(expected.year(), requisition.getRequestDateNpYear(),
                    "BS year drifted for " + date);
            date = date.plusDays(1);
        }
    }

    @Test
    @DisplayName("BS year always matches the leading year of the BS date string")
    void bsYearMatchesBsDatePrefix() {
        // The index on request_date_np_year is only useful if the two can never drift.
        LocalDate date = LocalDate.of(2025, 3, 1);
        LocalDate end = LocalDate.of(2027, 2, 28);

        while (!date.isAfter(end)) {
            PurchaseRequisition requisition = synced(date, null);
            String np = requisition.getRequestDateNp();

            if (np != null) {
                assertEquals(Integer.parseInt(np.substring(0, 4)),
                        requisition.getRequestDateNpYear(),
                        "request_date_np_year disagrees with request_date_np for " + date);
                assertTrue(requisition.getRequestDateNpYear() >= 2000,
                        "unexpected BS year for " + date);
            }
            date = date.plusDays(1);
        }
    }
}
