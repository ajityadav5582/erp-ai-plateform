package com.erp.platform.common.nepali.date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the JPA embeddable that stores AD and BS side by side.
 *
 * <p>The central guarantee is that the BS projection can never drift from the canonical
 * Gregorian date, which is precisely the defect that corrupted {@code master_bs_calendar}.
 */
class DualDateColumnsTest {

    @Test
    @DisplayName("Setting the AD date populates every BS column")
    void adDatePopulatesBsColumns() {
        DualDateColumns d = DualDateColumns.of(LocalDate.of(2024, 4, 13));
        assertEquals(LocalDate.of(2024, 4, 13), d.getAdDate());
        assertEquals("2081-01-01", d.getBsDate());
        assertEquals(2081, d.getBsYear());
        assertEquals(1, d.getBsMonth());
        assertEquals(1, d.getBsDay());
        assertFalse(d.getBsProvisional());
        assertTrue(d.isConsistent());
    }

    @Test
    @DisplayName("Entering a BS date converts it to the correct Gregorian date")
    void enteringBsDateConvertsToAd() {
        DualDateColumns d = DualDateColumns.ofBs("2081-01-01");
        assertEquals(LocalDate.of(2024, 4, 13), d.getAdDate());
        assertTrue(d.isConsistent());
    }

    @Test
    @DisplayName("Direct BS entry round-trips back to the same calendar date")
    void bsEntryRoundTrip() {
        DualDateColumns fromBs = DualDateColumns.ofBs("2082-01-01");
        assertEquals(LocalDate.of(2025, 4, 14), fromBs.getAdDate());
        assertEquals("2082-01-01", fromBs.getBsDate());
    }

    @Test
    @DisplayName("Mutating AD re-derives the BS columns")
    void mutatingAdRederivesBs() {
        DualDateColumns d = DualDateColumns.of(LocalDate.of(2024, 4, 13));
        d.setAdDate(LocalDate.of(2025, 4, 14));
        assertEquals("2082-01-01", d.getBsDate());
        assertEquals(2082, d.getBsYear());
        assertTrue(d.isConsistent());
    }

    @Test
    @DisplayName("Corrupting a BS column directly is repaired by sync(), not persisted")
    void syncRepairsCorruptedBsColumns() {
        DualDateColumns d = DualDateColumns.of(LocalDate.of(2024, 4, 13));
        // Simulate legacy drift: the component setters write a single field each and
        // deliberately do NOT recompute, so this is how a corrupt row looks in memory.
        d.setBsYear(2079);
        d.setBsMonth(4);
        d.setBsDay(1);
        assertFalse(d.isConsistent());

        d.sync(); // JPA @PrePersist / @PreUpdate

        assertEquals("2081-01-01", d.getBsDate());
        assertEquals(2081, d.getBsYear());
        assertTrue(d.isConsistent());
    }

    @Test
    @DisplayName("Provisional years are flagged on the row")
    void provisionalYearFlagged() {
        DualDateColumns d = DualDateColumns.of(LocalDate.of(2028, 6, 1));
        assertTrue(d.getBsProvisional());
        assertTrue(d.details().provisional());
    }

    @Test
    @DisplayName("A null AD date fails the lifecycle guard instead of writing a broken row")
    void nullAdRejectedBySync() {
        DualDateColumns d = DualDateColumns.of(LocalDate.of(2024, 4, 13));
        d.setAdDate(null);
        assertThrows(NepaliDateException.class, d::sync);
    }

    @Test
    @DisplayName("An AD date outside calendar support fails loudly")
    void unsupportedAdRejected() {
        assertThrows(NepaliDateException.class, () -> DualDateColumns.of(LocalDate.of(1800, 1, 1)));
    }

    @Test
    @DisplayName("Details expose both languages plus weekend metadata")
    void detailsExposeDisplayMetadata() {
        NepaliDateDetails details = DualDateColumns.of(LocalDate.of(2024, 4, 13)).details();
        assertEquals("2081-01-01", details.bsDate());
        assertEquals("२०८१-०१-०१", details.bsDateNp());
        assertEquals("Baisakh", details.monthNameEn());
        assertEquals("बैशाख", details.monthNameNp());
        assertEquals("Saturday", details.dayNameEn());
        assertTrue(details.weekend());
    }

    @Test
    @DisplayName("Equality is based on the calendar date, not object identity")
    void equalityByDate() {
        assertEquals(
                DualDateColumns.of(LocalDate.of(2024, 4, 13)),
                DualDateColumns.of(LocalDate.of(2024, 4, 13)));
        assertEquals(
                DualDateColumns.of(LocalDate.of(2024, 4, 13)).hashCode(),
                DualDateColumns.of(LocalDate.of(2024, 4, 13)).hashCode());
    }
}
