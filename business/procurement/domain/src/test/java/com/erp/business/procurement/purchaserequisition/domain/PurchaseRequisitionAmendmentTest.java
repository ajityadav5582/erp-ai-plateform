package com.erp.business.procurement.purchaserequisition.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers the aggregate behaviour this sprint added: fiscal-year ownership,
 * amending a draft's request date, refusing individual lines, and revising a
 * line in place.
 *
 * <p>Plain JUnit with no Spring and no database. Each of these is an invariant
 * the aggregate holds entirely in memory, so the fastest and most precise place
 * to state it is a direct unit test rather than a repository round trip.
 *
 * <p>The comments say what breaks if the assertion ever fails again, which is
 * the part a future reader actually needs from a test.
 */
class PurchaseRequisitionAmendmentTest {

    private static final LocalDate REQUEST_DATE = LocalDate.of(2026, 4, 15);
    private static final LocalDate REQUIRED_BY = LocalDate.of(2026, 5, 20);
    private static final long TENANT_ID = 1L;
    private static final long COMPANY_ID = 1L;
    private static final long FISCAL_YEAR = 7L;
    private static final long OTHER_FISCAL_YEAR = 8L;

    private static final Long LINE_ONE = 101L;
    private static final Long LINE_TWO = 102L;

    /** A scoped draft, two lines, 10 @ 100.00 and 5 @ 40.00, both at 13% tax. */
    private static PurchaseRequisition twoLineDraft() {
        PurchaseRequisition requisition = PurchaseRequisition.draft(
                "PR-2026/081-000001", 1L, 10L, REQUEST_DATE, REQUIRED_BY,
                RequisitionPriority.NORMAL, "NPR", null, null);
        requisition.assignScope(TENANT_ID, COMPANY_ID);
        requisition.addLine(PurchaseRequisitionLine.create(
                1, 100L, "ITM-001", "A4 Paper Ream", "PCS", null,
                new BigDecimal("10"), new BigDecimal("100.00"),
                new BigDecimal("13"), "Quarterly stationery"));
        requisition.addLine(PurchaseRequisitionLine.create(
                2, 200L, "ITM-002", "Toner cartridge", "PCS", null,
                new BigDecimal("5"), new BigDecimal("40.00"),
                new BigDecimal("13"), null));
        assignLineIds(requisition);
        return requisition;
    }

    /** Submits with a single approving step, leaving the document at SUBMITTED. */
    private static PurchaseRequisition twoLineSubmitted() {
        PurchaseRequisition requisition = twoLineDraft();
        requisition.submit(
                List.of(PurchaseRequisitionApproval.assignedToUser(1, 42L, "MANAGER", null, null)),
                "10");
        return requisition;
    }

    /**
     * Gives the in-memory lines the ids the database would have assigned.
     *
     * <p>Line-level outcomes are addressed by persisted id, so a fixture that
     * wants to exercise them has to have them. Reflection rather than an
     * id-accepting production factory, because a setter that exists only to make
     * a test convenient is a setter that production code will eventually use.
     */
    private static void assignLineIds(PurchaseRequisition requisition) {
        writeId(requisition.findLine(1), LINE_ONE);
        writeId(requisition.findLine(2), LINE_TWO);
    }

    private static void writeId(PurchaseRequisitionLine line, Long id) {
        // Declared on TenantScopedEntity rather than on the line, so the lookup
        // walks the hierarchy rather than inspecting the subclass alone.
        for (Class<?> type = PurchaseRequisitionLine.class; type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(line, id);
                return;
            } catch (NoSuchFieldException ignored) {
                // Declared further up.
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Could not write the line id", ex);
            }
        }
        throw new IllegalStateException("No 'id' field found on PurchaseRequisitionLine");
    }

    // ---- Fiscal year ownership ---------------------------------------------

    @Nested
    @DisplayName("assignFiscalYear")
    class FiscalYearAssignment {

        @Test
        @DisplayName("a document with no fiscal year is given the current one")
        void assignsToAnUnassignedDocument() {
            PurchaseRequisition requisition = twoLineDraft();

            requisition.assignFiscalYear(FISCAL_YEAR);

            assertEquals(FISCAL_YEAR, requisition.getFiscalYearId());
        }

        @Test
        @DisplayName("re-assigning the same fiscal year is a no-op")
        void isIdempotent() {
            PurchaseRequisition requisition = twoLineDraft();
            requisition.assignFiscalYear(FISCAL_YEAR);

            assertDoesNotThrow(() -> requisition.assignFiscalYear(FISCAL_YEAR));

            assertEquals(FISCAL_YEAR, requisition.getFiscalYearId());
        }

        @Test
        @DisplayName("a null fiscal year never clears an assignment")
        void nullDoesNotClear() {
            PurchaseRequisition requisition = twoLineDraft();
            requisition.assignFiscalYear(FISCAL_YEAR);

            requisition.assignFiscalYear(null);

            // Treated as "the caller has none", not "erase what is recorded".
            // Erasing would let a header with no fiscal-year context overwrite a
            // period that budget has already been reported against.
            assertEquals(FISCAL_YEAR, requisition.getFiscalYearId());
        }

        @Test
        @DisplayName("a draft may be moved to a different fiscal year")
        void draftMayBeMoved() {
            PurchaseRequisition requisition = twoLineDraft();
            requisition.assignFiscalYear(FISCAL_YEAR);

            assertDoesNotThrow(() -> requisition.assignFiscalYear(OTHER_FISCAL_YEAR));

            // Correcting a date that put the document in the wrong period is
            // legitimate; nothing has been committed against it yet.
            assertEquals(OTHER_FISCAL_YEAR, requisition.getFiscalYearId());
        }

        @Test
        @DisplayName("a submitted document may not be moved to a different fiscal year")
        void submittedMayNotBeMoved() {
            PurchaseRequisition requisition = twoLineSubmitted();
            requisition.assignFiscalYear(FISCAL_YEAR);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> requisition.assignFiscalYear(OTHER_FISCAL_YEAR));

            // Once a document is in the approval chain its value is being judged
            // against a period's budget. Moving it afterwards is how committed
            // spend ends up counted in two places, or in neither.
            assertTrue(thrown.getMessage().contains("cannot be moved to"), thrown.getMessage());
            assertEquals(FISCAL_YEAR, requisition.getFiscalYearId());
        }
    }

    // ---- Amending the request date -----------------------------------------

    @Nested
    @DisplayName("amendRequestDate")
    class RequestDateAmendment {

        @Test
        @DisplayName("a draft's request date can be corrected")
        void draftMayBeAmended() {
            PurchaseRequisition requisition = twoLineDraft();
            LocalDate corrected = LocalDate.of(2026, 4, 20);

            requisition.amendRequestDate(corrected);

            assertEquals(corrected, requisition.getRequestDate());
        }

        @Test
        @DisplayName("a submitted document's request date is frozen")
        void submittedMayNotBeAmended() {
            PurchaseRequisition requisition = twoLineSubmitted();

            // The date feeds the document number's fiscal period and the period
            // reports. Changing it after submission would re-file a document
            // that approvers have already judged.
            assertThrows(IllegalStateException.class,
                    () -> requisition.amendRequestDate(LocalDate.of(2026, 4, 20)));
        }

        @Test
        @DisplayName("a request date after the needed-by date is refused")
        void cannotBeMovedAfterTheNeededByDate() {
            PurchaseRequisition requisition = twoLineDraft();

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> requisition.amendRequestDate(REQUIRED_BY.plusDays(1)));

            // "Needed before it was raised" is not a document anybody can act on.
            assertTrue(thrown.getMessage().contains("falls after the needed-by date"),
                    thrown.getMessage());
        }

        @Test
        @DisplayName("a request date equal to the needed-by date is allowed")
        void equalToNeededByIsAllowed() {
            PurchaseRequisition requisition = twoLineDraft();

            assertDoesNotThrow(() -> requisition.amendRequestDate(REQUIRED_BY));

            assertEquals(REQUIRED_BY, requisition.getRequestDate());
        }
    }

    // ---- Line-level refusal and withdrawal --------------------------------

    @Nested
    @DisplayName("rejectLines and cancelLines")
    class LineOutcomes {

        @Test
        @DisplayName("a single-step document can still have a line refused")
        void singleStepChainAcceptsARefusal() {
            PurchaseRequisition requisition = twoLineSubmitted();

            assertDoesNotThrow(() -> requisition.rejectLines(List.of(LINE_TWO)));

            // A one-step chain goes straight from SUBMITTED to APPROVED. The
            // document is SUBMITTED for the whole of that queue, so guarding on
            // SUBMITTED alone is what makes line refusal possible for the small
            // requisitions that most need it.
            assertEquals(PurchaseRequisitionLineStatus.REJECTED,
                    requisition.findLineById(LINE_TWO).orElseThrow().getStatus());
        }

        @Test
        @DisplayName("a refused line drops out of the estimate straight away")
        void refusedLineLeavesTheEstimate() {
            PurchaseRequisition requisition = twoLineSubmitted();

            requisition.rejectLines(List.of(LINE_TWO));

            // Line two is 5 @ 40.00 plus 13% tax = 226.00 of a 1356.00 estimate.
            // The estimate is what the header and the list show, so a refused line
            // left in it would keep inflating the document's apparent value.
            assertEquals(0, new BigDecimal("1130.00").compareTo(requisition.getEstimatedTotalAmount()),
                    requisition.getEstimatedTotalAmount().toPlainString());
        }

        @Test
        @DisplayName("a refused line contributes nothing once the document is approved")
        void refusedLineContributesNoApprovedValue() {
            PurchaseRequisition requisition = twoLineSubmitted();
            requisition.findLine(1).approveQuantity(new BigDecimal("10"));
            requisition.findLine(2).approveQuantity(new BigDecimal("5"));
            requisition.rejectLines(List.of(LINE_TWO));
            requisition.approve("42");

            // Line one alone: 10 @ 100.00 plus 13% tax = 1130.00. Leaving the
            // refused line in would let procurement be awarded against demand the
            // approver said no to.
            assertEquals(0, new BigDecimal("1130.00").compareTo(requisition.getApprovedTotalAmount()),
                    requisition.getApprovedTotalAmount().toPlainString());
        }

        @Test
        @DisplayName("approved value stays at zero until the document is actually approved")
        void approvedValueIsZeroWhilePending() {
            PurchaseRequisition requisition = twoLineSubmitted();
            requisition.findLine(1).approveQuantity(new BigDecimal("10"));
            requisition.recalculateTotals();

            // A grant on a line is still a proposal while the document is in the
            // queue. Reporting it as committed budget would let a pending request
            // consume a period twice: once here and again when it is approved.
            assertEquals(0, BigDecimal.ZERO.compareTo(requisition.getApprovedTotalAmount()),
                    requisition.getApprovedTotalAmount().toPlainString());
        }

        @Test
        @DisplayName("a withdrawn line is distinguished from a refused one")
        void withdrawnLineIsRecordedDistinctly() {
            PurchaseRequisition requisition = twoLineSubmitted();

            requisition.cancelLines(List.of(LINE_TWO));

            // Same arithmetic, different audit fact: "the approver said no" and
            // "we no longer need this" are different statements.
            assertEquals(PurchaseRequisitionLineStatus.CANCELLED,
                    requisition.findLineById(LINE_TWO).orElseThrow().getStatus());
            assertFalse(requisition.findLineById(LINE_TWO).orElseThrow().isCounted());
        }

        @Test
        @DisplayName("a draft cannot refuse its own lines")
        void draftCannotRefuseLines() {
            PurchaseRequisition requisition = twoLineDraft();

            // Before submission there is no approver to have refused anything. The
            // requester deletes the line instead.
            assertThrows(IllegalStateException.class,
                    () -> requisition.rejectLines(List.of(LINE_TWO)));
        }

        @Test
        @DisplayName("an approved document cannot refuse its lines after the fact")
        void approvedCannotRefuseLines() {
            PurchaseRequisition requisition = twoLineSubmitted();
            requisition.findLine(1).approveQuantity(new BigDecimal("10"));
            requisition.findLine(2).approveQuantity(new BigDecimal("5"));
            requisition.approve("42");

            assertThrows(IllegalStateException.class,
                    () -> requisition.rejectLines(List.of(LINE_TWO)));
        }

        @Test
        @DisplayName("a line id from another requisition is refused")
        void foreignLineIdIsRefused() {
            PurchaseRequisition requisition = twoLineSubmitted();

            // Better to say "no such line" than to silently refuse nothing and
            // record an approval the approver believes refused a line.
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> requisition.rejectLines(List.of(999L)));

            assertTrue(thrown.getMessage().contains("has no line with id 999"), thrown.getMessage());
        }

        @Test
        @DisplayName("an empty refusal list is refused")
        void emptyListIsRefused() {
            PurchaseRequisition requisition = twoLineSubmitted();

            assertThrows(IllegalArgumentException.class,
                    () -> requisition.rejectLines(List.of()));
        }

        @Test
        @DisplayName("the same line listed twice is refused")
        void duplicateIsRefused() {
            PurchaseRequisition requisition = twoLineSubmitted();

            assertThrows(IllegalArgumentException.class,
                    () -> requisition.rejectLines(List.of(LINE_TWO, LINE_TWO)));
        }
    }

    // ---- Revising a line in place ------------------------------------------

    @Nested
    @DisplayName("revising a line")
    class LineRevision {

        @Test
        @DisplayName("a revised line carries the new values")
        void revisionAppliesNewValues() {
            PurchaseRequisition requisition = twoLineDraft();

            requisition.findLine(1).revise(
                    100L, "ITM-001", "A4 Paper Ream", "PCS", null,
                    new BigDecimal("20"), new BigDecimal("95.00"),
                    new BigDecimal("13"), "Twice the quantity");

            PurchaseRequisitionLine line = requisition.findLine(1);
            assertEquals(0, new BigDecimal("20").compareTo(line.getRequestedQuantity()));
            assertEquals(0, new BigDecimal("95.00").compareTo(line.getEstimatedUnitPrice()));
            assertEquals("Twice the quantity", line.getRemarks());
        }

        @Test
        @DisplayName("a revised line drops any stale grant")
        void revisionClearsTheGrant() {
            PurchaseRequisition requisition = twoLineDraft();
            requisition.findLine(1).approveQuantity(new BigDecimal("10"));
            requisition.findLine(2).approveQuantity(new BigDecimal("5"));

            requisition.findLine(1).revise(
                    100L, "ITM-001", "A4 Paper Ream", "PCS", null,
                    new BigDecimal("20"), new BigDecimal("95.00"),
                    new BigDecimal("13"), null);

            // Granting 10 of a line the requester has now asked for 20 of is not
            // a partial approval, it is a stale approval. The quantity has to go
            // back to zero so the next approver decides afresh.
            PurchaseRequisitionLine line = requisition.findLine(1);
            assertEquals(0, BigDecimal.ZERO.compareTo(line.getApprovedQuantity()));
            assertEquals(PurchaseRequisitionLineStatus.REQUESTED, line.getStatus());
        }

        @Test
        @DisplayName("a revision with no quantity is refused")
        void nonPositiveQuantityIsRefused() {
            PurchaseRequisition requisition = twoLineDraft();

            assertThrows(IllegalArgumentException.class,
                    () -> requisition.findLine(1).revise(
                            100L, "ITM-001", "A4 Paper Ream", "PCS", null,
                            BigDecimal.ZERO, new BigDecimal("95.00"),
                            new BigDecimal("13"), null));
        }
    }

    // ---- Lookup ------------------------------------------------------------

    @Nested
    @DisplayName("line lookup")
    class LineLookup {

        @Test
        @DisplayName("a line is found by its persisted id")
        void findsById() {
            PurchaseRequisition requisition = twoLineDraft();

            assertEquals(1, requisition.findLineById(LINE_ONE).orElseThrow().getLineNumber());
        }

        @Test
        @DisplayName("an unknown id resolves to empty rather than throwing")
        void unknownIdIsEmpty() {
            PurchaseRequisition requisition = twoLineDraft();

            // Lookup is the non-throwing form; requireLineById is the strict one.
            assertTrue(requisition.findLineById(999L).isEmpty());
        }

        @Test
        @DisplayName("a line is found by its sequence number")
        void findsByNumber() {
            PurchaseRequisition requisition = twoLineDraft();

            assertEquals(LINE_TWO, requisition.findLineByNumber(2).orElseThrow().getId());
        }

        @Test
        @DisplayName("removing a line drops it from the aggregate")
        void removeDropsTheLine() {
            PurchaseRequisition requisition = twoLineDraft();

            requisition.removeLine(2);

            assertTrue(requisition.findLineByNumber(2).isEmpty());
            assertEquals(1, requisition.getLines().size());
        }

        @Test
        @DisplayName("removing a line that is not there is refused")
        void removeUnknownLineIsRefused() {
            PurchaseRequisition requisition = twoLineDraft();

            assertThrows(IllegalArgumentException.class, () -> requisition.removeLine(9));
        }
    }

    // ---- Draft without a fiscal year ---------------------------------------

    @Nested
    @DisplayName("documents raised before a fiscal year was selected")
    class MissingFiscalYear {

        @Test
        @DisplayName("a draft can be recorded with no fiscal year at all")
        void draftMayHaveNoFiscalYear() {
            PurchaseRequisition requisition = twoLineDraft();

            assertNull(requisition.getFiscalYearId());

            // The service logs rather than refuses: blocking the write would mean
            // a missing header stops a branch raising a requisition at all, which
            // is a worse outcome than a reportable gap.
            assertDoesNotThrow(() -> requisition.assignFiscalYear(null));
            assertNull(requisition.getFiscalYearId());
        }
    }
    // ---- Currency on amendment ---------------------------------------------

    /**
     * Currency was settable only at construction, so the update endpoint accepted a
     * currency and dropped it. A requester who picked the wrong one was told the
     * save worked and found the old value on reload. These pin the three cases that
     * bug could hide behind: a genuine change that must land, and the two ways a
     * caller says "I did not supply this" which must both be no-ops.
     */
    @Nested
    @DisplayName("amending the currency")
    class Currency {

        /** A draft with no lines - currency is a header field and needs none. */
        private PurchaseRequisition draftIn(String currency) {
            PurchaseRequisition requisition = PurchaseRequisition.draft(
                    "PR-2026/081-000009", 1L, 10L, REQUEST_DATE, REQUIRED_BY,
                    RequisitionPriority.NORMAL, currency, null, null);
            requisition.assignScope(TENANT_ID, COMPANY_ID);
            return requisition;
        }

        private void amendTo(PurchaseRequisition requisition, String currency) {
            requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, currency,
                    null, null, null, null, null);
        }

        @Test
        @DisplayName("a supplied currency is applied and normalised")
        void suppliedCurrencyIsApplied() {
            PurchaseRequisition requisition = draftIn("NPR");

            amendTo(requisition, "usd");

            // Normalised, not merely assigned: "usd" and "USD" must not end up as
            // two distinct currencies in the same column.
            assertEquals("USD", requisition.getCurrency());
        }

        @Test
        @DisplayName("a null currency leaves the current one alone")
        void nullCurrencyKeepsTheCurrentOne() {
            PurchaseRequisition requisition = draftIn("USD");

            amendTo(requisition, null);

            // Not "NPR". Clearing would re-denominate every amount on the document
            // without a number changing - a 5,000 rupee estimate becoming 5,000
            // dollars - on the strength of a field the caller merely omitted.
            assertEquals("USD", requisition.getCurrency());
        }

        @Test
        @DisplayName("a blank currency leaves the current one alone, exactly as null does")
        void blankCurrencyKeepsTheCurrentOne() {
            PurchaseRequisition requisition = draftIn("USD");

            amendTo(requisition, "   ");

            // A form field the user cleared submits "", not null. Treating the two
            // spellings of "not supplied" differently would make the safer one
            // unreachable in practice.
            assertEquals("USD", requisition.getCurrency());
        }

        @Test
        @DisplayName("a malformed currency is refused rather than stored")
        void malformedCurrencyIsRefused() {
            PurchaseRequisition requisition = draftIn("NPR");

            assertThrows(IllegalArgumentException.class, () -> amendTo(requisition, "RUPEE"));

            // The rejection has to leave the document as it was, not half-applied.
            assertEquals("NPR", requisition.getCurrency());
        }
    }

    // ---- Warehouse on amendment --------------------------------------------

    /**
     * The warehouse follows exactly the same rules as the currency, and for exactly
     * the same reason: a client that omits the field in a partial update must not have
     * it silently cleared. Losing a nominated delivery site because somebody saved an
     * unrelated line change is the same class of defect as losing a currency, and the
     * old free-text address fields had no protection against it at all.
     */
    @Nested
    @DisplayName("amending the destination warehouse")
    class Warehouse {

        private static final Long KATHMANDU = 5L;
        private static final Long POKHARA = 6L;

        /** A draft nominated to Kathmandu. */
        private PurchaseRequisition draftTo(Long warehouseId, String code, String name) {
            PurchaseRequisition requisition = PurchaseRequisition.draft(
                    "PR-2026/081-000010", 1L, 10L, REQUEST_DATE, REQUIRED_BY,
                    RequisitionPriority.NORMAL, "NPR", null, null);
            requisition.assignScope(TENANT_ID, COMPANY_ID);
            requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, "NPR",
                    warehouseId, code, name, null, null);
            return requisition;
        }

        @Test
        @DisplayName("a supplied warehouse replaces the nomination, snapshot included")
        void suppliedWarehouseIsApplied() {
            PurchaseRequisition requisition = draftTo(KATHMANDU, "KTM-01", "Kathmandu Central");

            requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, "NPR",
                    POKHARA, "PKR-02", "Pokhara Depot", null, null);

            assertEquals(POKHARA, requisition.getWarehouseId());
            // The snapshot moves with the id. A document pointing at 6 but printing
            // "Kathmandu Central" is worse than one printing nothing, because the
            // print looks authoritative.
            assertEquals("PKR-02", requisition.getWarehouseCode());
            assertEquals("Pokhara Depot", requisition.getWarehouseName());
        }

        @Test
        @DisplayName("an omitted warehouse leaves the nomination alone")
        void nullWarehouseKeepsTheCurrentOne() {
            PurchaseRequisition requisition = draftTo(KATHMANDU, "KTM-01", "Kathmandu Central");

            requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, "NPR",
                    null, null, null, null, null);

            // Saving a corrected line must not quietly un-nominate the site the goods
            // are going to. This is the case the free-text fields could not express.
            assertEquals(KATHMANDU, requisition.getWarehouseId());
            assertEquals("Kathmandu Central", requisition.getWarehouseName());
        }

        @Test
        @DisplayName("a non-positive warehouse id is refused")
        void nonPositiveWarehouseIsRefused() {
            PurchaseRequisition requisition = draftTo(KATHMANDU, "KTM-01", "Kathmandu Central");

            assertThrows(IllegalArgumentException.class,
                    () -> requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, "NPR",
                            0L, "X", "Nowhere", null, null));

            assertEquals(KATHMANDU, requisition.getWarehouseId());
        }

        @Test
        @DisplayName("a warehouse id with no snapshot is refused")
        void halfSuppliedWarehouseIsRefused() {
            PurchaseRequisition requisition = draftTo(KATHMANDU, "KTM-01", "Kathmandu Central");

            // An id with no name is an unresolved reference. Accepting it would freeze
            // half a snapshot, and the missing half can only be recovered by calling
            // another service at read time - which is what the snapshot exists to avoid.
            assertThrows(IllegalArgumentException.class,
                    () -> requisition.amendDetails(REQUIRED_BY, RequisitionPriority.NORMAL, "NPR",
                            POKHARA, null, null, null, null));

            assertEquals(KATHMANDU, requisition.getWarehouseId());
        }

        @Test
        @DisplayName("a requisition with no warehouse at all is allowed")
        void noWarehouseIsAllowed() {
            // Optional on purpose: the requester often does not know the site yet,
            // and refusing the document over it would block a branch entirely.
            PurchaseRequisition requisition = draftTo(null, null, null);

            assertNull(requisition.getWarehouseId());
        }
    }
}
