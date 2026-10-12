package com.erp.business.procurement.purchaserequisition.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the purchase requisition totals and ordering rules.
 *
 * <p>Each test here corresponds to a defect that shipped and was not caught,
 * because the module previously had no coverage at all for the money paths. The
 * comments say what breaks if the assertion ever fails again, which is the part
 * a future reader actually needs.
 *
 * <p>Plain JUnit, no Spring and no database: the aggregate holds its invariants
 * in memory, so testing it directly is both faster and a more precise statement
 * of intent than a repository round trip would be.
 */
class PurchaseRequisitionTotalsTest {

    private static final LocalDate REQUEST_DATE = LocalDate.of(2026, 4, 1);
    private static final long TENANT_ID = 1L;
    private static final long COMPANY_ID = 1L;

    /** 10 units at 100.00 with 13% tax → net 1000.00, tax 130.00, gross 1130.00. */
    private static final BigDecimal NET = new BigDecimal("1000.00");
    private static final BigDecimal TAX = new BigDecimal("130.00");
    private static final BigDecimal GROSS = new BigDecimal("1130.00");

    /**
     * Asserts two money amounts are numerically equal.
     *
     * <p>Uses {@code compareTo} rather than {@code equals} on purpose. BigDecimal
     * equality is scale-sensitive, so {@code 1000.0000.equals(1000.00)} is false
     * even though the two amounts are the same money. Asserting money with
     * {@code equals} produces failures that look like arithmetic bugs and invite
     * someone to "fix" working code.
     */
    private static void assertMoney(BigDecimal expected, BigDecimal actual, String what) {
        assertEquals(0, expected.compareTo(actual),
                what + ": expected " + expected.toPlainString()
                        + " but was " + actual.toPlainString());
    }

    /** A scoped draft with one line: 10 @ 100.00, 13% tax. */
    private static PurchaseRequisition draftWithOneLine() {
        PurchaseRequisition requisition = PurchaseRequisition.draft(
                "PR-TEST-0001", 1L, 10L, REQUEST_DATE, null,
                RequisitionPriority.NORMAL, "NPR", null, null);
        requisition.assignScope(TENANT_ID, COMPANY_ID);
        requisition.addLine(PurchaseRequisitionLine.create(
                1, 100L, "ITM-001", "A4 Paper Ream", "PCS", null,
                new BigDecimal("10"), new BigDecimal("100.00"),
                new BigDecimal("13"), "Quarterly stationery"));
        return requisition;
    }

    /** Submits, grants the requested quantity in full, and approves the document. */
    private static PurchaseRequisition submittedAndFullyApproved() {
        PurchaseRequisition requisition = draftWithOneLine();
        requisition.submit(
                List.of(PurchaseRequisitionApproval.assignedToUser(1, 42L, "MANAGER", null, null)),
                "10");
        requisition.findLine(1).approveQuantity(new BigDecimal("10"));
        requisition.approve("42");
        return requisition;
    }

    @Nested
    @DisplayName("Estimated totals are derived from the requested quantity")
    class EstimatedTotals {

        @Test
        @DisplayName("a fresh draft already carries a non-zero estimate")
        void draftEstimateIsNotZero() {
            // The regression: the estimate used to be derived from the approved
            // quantity, which is zero on every line until someone approves. Every
            // draft therefore read as worth nothing, and the amount-band approval
            // policy resolved every requisition - of any value - against the lowest
            // band. A large request bypassed its CFO step entirely.
            PurchaseRequisition requisition = draftWithOneLine();

            assertMoney(NET, requisition.getEstimatedSubtotalAmount(), "estimated subtotal");
            assertMoney(TAX, requisition.getEstimatedTaxAmount(), "estimated tax");
            assertMoney(GROSS, requisition.getEstimatedTotalAmount(), "estimated total");
        }

        @Test
        @DisplayName("nothing is committed to budget before approval")
        void draftDoesNotConsumeBudget() {
            // The counterpart to the fix above. An estimate answers "what is being
            // asked for"; approvedTotalAmount answers "what has the company
            // committed to". Deriving the first from the second would have made the
            // budget dashboard read a proposal as a commitment.
            PurchaseRequisition requisition = draftWithOneLine();

            assertEquals(0, requisition.getApprovedTotalAmount().signum());
            assertEquals(0, requisition.getOrderedTotalAmount().signum());
        }

        @Test
        @DisplayName("a rejected line drops out of the estimate")
        void rejectedLineIsExcluded() {
            PurchaseRequisition requisition = draftWithOneLine();
            requisition.addLine(PurchaseRequisitionLine.create(
                    2, 200L, "ITM-002", "Toner Cartridge", "PCS", null,
                    new BigDecimal("2"), new BigDecimal("500.00"),
                    new BigDecimal("13"), null));

            requisition.findLine(2).rejectLine();
            requisition.recalculateTotals();

            // Both lines are 1000.00 net, so the header staying at 1000.00 is the
            // proof that the rejected line was excluded from the estimate.
            assertMoney(NET, requisition.getEstimatedSubtotalAmount(), "estimated subtotal");
        }
    }

    @Nested
    @DisplayName("Approved totals sit on the same gross basis as the estimate")
    class ApprovedTotals {

        @Test
        @DisplayName("approved total is net plus tax once the document is approved")
        void approvedTotalIsGross() {
            // The regression: approvedTotalAmount was net-only while
            // estimatedTotalAmount was net plus tax. A budget report comparing the
            // two understated every commitment by exactly the tax - 130.00 here,
            // which on a real spend is not a rounding artefact.
            PurchaseRequisition requisition = submittedAndFullyApproved();

            assertMoney(GROSS, requisition.getApprovedTotalAmount(), "approved total");
        }

        @Test
        @DisplayName("a partial grant commits only what was granted")
        void partialGrantCommitsOnlyGrantedQuantity() {
            PurchaseRequisition requisition = draftWithOneLine();
            requisition.submit(
                    List.of(PurchaseRequisitionApproval.assignedToUser(1, 42L, "MANAGER", null, null)),
                    "10");
            requisition.findLine(1).approveQuantity(new BigDecimal("5"));
            requisition.approve("42");

            // 5 of 10 units is half of both the net and the gross.
            assertMoney(new BigDecimal("565.00"), requisition.getApprovedTotalAmount(),
                    "approved total on a partial grant");
        }

        @Test
        @DisplayName("outstanding value is the approved total less what is already ordered")
        void outstandingTracksUnorderedCommitment() {
            // 6 of 10 units still un-ordered is 60% of the 1130.00 committed.
            PurchaseRequisition requisition = submittedAndFullyApproved();
            requisition.findLine(1).registerOrdered(new BigDecimal("4"));
            requisition.recalculateTotals();

            assertMoney(new BigDecimal("678.00"), requisition.outstandingValue(),
                    "outstanding value after a partial order");
        }
    }

    @Nested
    @DisplayName("Conversion to a purchase order is a single, terminal move")
    class ConversionToPurchaseOrder {

        @Test
        @DisplayName("converting an approved requisition lands on CONVERTED_TO_PO")
        void approvedRequisitionConverts() {
            PurchaseRequisition requisition = submittedAndFullyApproved();
            requisition.findLine(1).registerOrdered(new BigDecimal("10"));

            requisition.convertToPO("42");

            assertEquals(PurchaseRequisitionStatus.CONVERTED_TO_PO, requisition.getStatus());
            assertEquals("42", requisition.getConvertedBy());
            assertMoney(BigDecimal.ZERO, requisition.outstandingValue(),
                    "nothing outstanding once every line has been ordered");
        }

        @Test
        @DisplayName("the committed total survives conversion")
        void conversionKeepsTheCommittedTotal() {
            // Budget is committed at approval. CONVERTED_TO_PO is a record of what
            // happened to that commitment, not a release of it, so the figure a
            // period report reads must not drop when the document is converted.
            PurchaseRequisition requisition = submittedAndFullyApproved();
            requisition.findLine(1).registerOrdered(new BigDecimal("10"));

            requisition.convertToPO("42");

            assertMoney(GROSS, requisition.getApprovedTotalAmount(),
                    "approved total after conversion");
        }

        @Test
        @DisplayName("a second conversion is refused rather than silently accepted")
        void conversionHappensOnlyOnce() {
            // The regression behind the 500. A terminal state makes a second award an
            // explicit refusal instead of a server fault.
            PurchaseRequisition requisition = submittedAndFullyApproved();
            requisition.findLine(1).registerOrdered(new BigDecimal("4"));
            requisition.convertToPO("42");

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> requisition.convertToPO("42"));

            assertTrue(thrown.getMessage().contains("APPROVED"), thrown.getMessage());
            assertEquals(PurchaseRequisitionStatus.CONVERTED_TO_PO, requisition.getStatus());
        }

        @Test
        @DisplayName("conversion is refused before the document is approved")
        void cannotConvertBeforeApproval() {
            PurchaseRequisition requisition = draftWithOneLine();

            assertThrows(IllegalStateException.class, () -> requisition.convertToPO("42"));
        }

        @Test
        @DisplayName("ordering is refused once quantity has been rejected")
        void cannotOrderAgainstRejectedLine() {
            PurchaseRequisition requisition = submittedAndFullyApproved();
            requisition.findLine(1).rejectLine();

            assertThrows(IllegalStateException.class,
                    () -> requisition.findLine(1).registerOrdered(new BigDecimal("1")));
        }
    }

    @Nested
    @DisplayName("Currency codes are validated rather than coerced")
    class CurrencyValidation {

        @Test
        @DisplayName("a lowercase code is normalised, not rejected")
        void lowercaseIsNormalised() {
            PurchaseRequisition requisition = PurchaseRequisition.draft(
                    "PR-TEST-0002", 1L, 10L, REQUEST_DATE, null,
                    RequisitionPriority.NORMAL, "npr", null, null);

            assertEquals("NPR", requisition.getCurrency());
        }

        @Test
        @DisplayName("a malformed code is rejected outright")
        void malformedCodeIsRejected() {
            // The regression: the old code uppercased and truncated whatever it was
            // given, so "NPRS" and "rupees" both became "NPR". A document silently
            // denominated in the wrong currency is a far worse outcome than a
            // rejected request, because nothing downstream would ever report it.
            assertThrows(IllegalArgumentException.class, () -> PurchaseRequisition.draft(
                    "PR-TEST-0003", 1L, 10L, REQUEST_DATE, null,
                    RequisitionPriority.NORMAL, "NPRS", null, null));

            assertThrows(IllegalArgumentException.class, () -> PurchaseRequisition.draft(
                    "PR-TEST-0004", 1L, 10L, REQUEST_DATE, null,
                    RequisitionPriority.NORMAL, "rupees", null, null));
        }

        @Test
        @DisplayName("an absent code still defaults to NPR")
        void absentCodeDefaultsToNpr() {
            // Blank is deliberately *not* rejected. Rejection is for a value the
            // client supplied and got wrong; absence is a legitimate call for a
            // single-country deployment, and making every client send "NPR"
            // explicitly buys nothing.
            assertEquals("NPR", draftWithCurrency(null).getCurrency());
            assertEquals("NPR", draftWithCurrency("").getCurrency());
            assertEquals("NPR", draftWithCurrency("   ").getCurrency());
        }

        private PurchaseRequisition draftWithCurrency(String currency) {
            return PurchaseRequisition.draft(
                    "PR-TEST-0006", 1L, 10L, REQUEST_DATE, null,
                    RequisitionPriority.NORMAL, currency, null, null);
        }
    }

    @Nested
    @DisplayName("Role-assigned approval steps can be matched")
    class RoleAssignedSteps {

        @Test
        @DisplayName("the frozen role name matches case-insensitively")
        void matchesOnRoleName() {
            PurchaseRequisitionApproval step = PurchaseRequisitionApproval.assignedToRole(
                    1, 7L, "PROCUREMENT_MANAGER", new BigDecimal("500000.00"), null);

            assertTrue(step.isRoleAssigned());
            assertTrue(step.isRoleHeldBy(List.of("PROCUREMENT_MANAGER")));
            assertTrue(step.isRoleHeldBy(List.of("procurement_manager")),
                    "identity providers are inconsistent about role-name casing");
            assertTrue(step.isRoleHeldBy(List.of("EMPLOYEE", "  procurement_manager  ")),
                    "surrounding whitespace in a token claim should not defeat the match");
        }

        @Test
        @DisplayName("the numeric role id matches when the token carries ids, not names")
        void matchesOnRoleId() {
            PurchaseRequisitionApproval step = PurchaseRequisitionApproval.assignedToRole(
                    1, 7L, "PROCUREMENT_MANAGER", null, null);

            assertTrue(step.isRoleHeldBy(List.of("7")));
        }

        @Test
        @DisplayName("a user without the role is refused")
        void refusesNonHolder() {
            PurchaseRequisitionApproval step = PurchaseRequisitionApproval.assignedToRole(
                    1, 7L, "PROCUREMENT_MANAGER", null, null);

            assertFalse(step.isRoleHeldBy(List.of("STORE_KEEPER")));
            assertFalse(step.isRoleHeldBy(List.of()));
            assertFalse(step.isRoleHeldBy(null));
        }

        @Test
        @DisplayName("a user-assigned step is not matchable by role")
        void userAssignedStepIgnoresRoles() {
            // Guards against a privilege widening: holding a role must never let a
            // caller act on a step that names one specific person.
            PurchaseRequisitionApproval step = PurchaseRequisitionApproval.assignedToUser(
                    1, 42L, "MANAGER", null, null);

            assertTrue(step.isAssignedTo(42L));
            assertFalse(step.isAssignedTo(43L));
            assertFalse(step.isRoleHeldBy(List.of("MANAGER")),
                    "a person-assigned step must not become role-assigned");
        }
    }
}
