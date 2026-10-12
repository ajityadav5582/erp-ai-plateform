package com.erp.business.procurement.purchaseorder.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for goods receipt against a purchase order.
 *
 * <p><strong>The defect these exist for.</strong> Receiving goods threw
 * {@code "Goods cannot be received against a purchase order in state RECEIVED"}
 * on a perfectly valid order. The header status was derived from the single line
 * that had just been received, so completing <em>one</em> line of a multi line
 * order promoted the whole order to RECEIVED and made the lines still outstanding
 * permanently unreceivable. Every test below is about the status following the
 * whole line set rather than the last line touched.
 *
 * <p>Plain JUnit, no Spring and no database: the aggregate holds its invariants in
 * memory, so testing it directly is both faster and a more precise statement of
 * intent than a repository round trip would be.
 */
class PurchaseOrderReceiptTest {

    private static final LocalDate ORDER_DATE = LocalDate.of(2026, 4, 1);
    private static final long TENANT_ID = 1L;
    private static final long COMPANY_ID = 1L;
    private static final BigDecimal TEN = new BigDecimal("10");
    private static final BigDecimal TWENTY = new BigDecimal("20");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /**
     * An approved order carrying the given line quantities, numbered from 1.
     *
     * <p>Each line is N units at 100.00 with no discount or tax, so its net amount
     * is quantity x 100 and the arithmetic in each test stays obvious.
     */
    private static PurchaseOrder approvedOrderWithLines(BigDecimal... quantities) {
        PurchaseOrder order = PurchaseOrder.draft(
                "PO-TEST-0001", ORDER_DATE, 10L, "SUP-001", "Test Supplier", "NPR", 1L);
        order.assignScope(TENANT_ID, COMPANY_ID);

        for (int i = 0; i < quantities.length; i++) {
            order.addLine(PurchaseOrderLine.create(
                    i + 1, 100L + i, "ITEM-" + (i + 1), "Item " + (i + 1), "PCS", null,
                    quantities[i], HUNDRED, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        order.transitionTo(PurchaseOrderStatus.SUBMITTED, "buyer", null);
        order.transitionTo(PurchaseOrderStatus.APPROVED, "approver", null);
        return order;
    }

    /**
     * Asserts two money amounts are numerically equal.
     *
     * <p>Uses {@code compareTo} rather than {@code equals}, because BigDecimal
     * equality is scale-sensitive: {@code 1000.0000.equals(1000.00)} is false even
     * though both are the same money.
     */
    private static void assertMoney(BigDecimal expected, BigDecimal actual, String what) {
        assertEquals(0, expected.compareTo(actual),
                what + ": expected " + expected.toPlainString()
                        + " but was " + actual.toPlainString());
    }

    @Nested
    @DisplayName("Header status follows the whole line set")
    class HeaderStatusDerivation {

        @Test
        @DisplayName("completing one line of three leaves the order receivable")
        void completingOneLineOfManyKeepsOrderReceivable() {
            // The reported failure, reduced to its smallest form: line 1 arrives in
            // full while lines 2 and 3 have not been touched at all.
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN, TEN);

            PurchaseOrderStatus after = order.receiveAgainstLine(1, TEN);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, after,
                    "A fully received line must not promote an order whose other lines are outstanding");
            assertTrue(order.isReceivable(),
                    "The order must still accept deliveries while lines 2 and 3 are outstanding");
        }

        @Test
        @DisplayName("the lines that were still outstanding can then be received")
        void outstandingLinesRemainReceivableAfterOneLineCompletes() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN, TEN);
            order.receiveAgainstLine(1, TEN);

            // This call is what used to throw INVALID_STATE on state RECEIVED.
            PurchaseOrderStatus afterSecond = order.receiveAgainstLine(2, TEN);
            PurchaseOrderStatus afterThird = order.receiveAgainstLine(3, TEN);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, afterSecond);
            assertEquals(PurchaseOrderStatus.RECEIVED, afterThird,
                    "The order reaches RECEIVED only once every line is complete");
            assertMoney(new BigDecimal("10"), order.findLine(1).getReceivedQuantity(),
                    "line 1 received quantity");
            assertMoney(new BigDecimal("10"), order.findLine(3).getReceivedQuantity(),
                    "line 3 received quantity");
        }

        @Test
        @DisplayName("the last line to complete, not the first, decides RECEIVED")
        void statusDependsOnEveryLineNotTheLastReceived() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TWENTY);

            order.receiveAgainstLine(2, TWENTY);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus(),
                    "Line 1 has not arrived, so the order is not fully received even though "
                            + "the line just received in this call was complete");
            assertMoney(new BigDecimal("2000"), order.getReceivedAmount(),
                    "Only line 2's value should have been recognised");
        }

        @Test
        @DisplayName("a fully received order refuses further receipt, saying why")
        void fullyReceivedOrderRefusesFurtherReceipt() {
            PurchaseOrder order = approvedOrderWithLines(TEN);
            order.receiveAgainstLine(1, TEN);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> order.receiveAgainstLine(1, new BigDecimal("0.001")));

            assertTrue(thrown.getMessage().contains("nothing left to receive"),
                    "The refusal should tell the operator there is nothing outstanding, "
                            + "not report a state conflict they cannot act on. Was: "
                            + thrown.getMessage());
        }

        @Test
        @DisplayName("an approved order that has received nothing stays APPROVED")
        void orderWithNoReceiptsStaysApproved() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN);

            assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus(),
                    "A receipt call must not promote an order merely because it happened");
            assertMoney(BigDecimal.ZERO, order.getReceivedAmount(),
                    "Nothing has arrived so nothing should be recognised as received");
        }
    }

    @Nested
    @DisplayName("Partial and short deliveries")
    class PartialDeliveries {

        @Test
        @DisplayName("a short delivery leaves both the line and the order partial")
        void shortDeliveryIsRecordedAsPartial() {
            PurchaseOrder order = approvedOrderWithLines(TWENTY);

            PurchaseOrderStatus after = order.receiveAgainstLine(1, TEN);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, after);
            assertEquals(PurchaseOrderLineStatus.PARTIALLY_RECEIVED, order.findLine(1).getStatus());
            assertMoney(new BigDecimal("1000"), order.getReceivedAmount(),
                    "Half the line arrived, so half its value is recognised");
            assertMoney(new BigDecimal("1000"), order.outstandingAmount(),
                    "The other half is still owed");
        }

        @Test
        @DisplayName("the balance of a short delivery completes the order")
        void balanceOfAShortDeliveryCompletesTheOrder() {
            PurchaseOrder order = approvedOrderWithLines(TWENTY);
            order.receiveAgainstLine(1, TEN);

            PurchaseOrderStatus after = order.receiveAgainstLine(1, TEN);

            assertEquals(PurchaseOrderStatus.RECEIVED, after);
            assertEquals(PurchaseOrderLineStatus.RECEIVED, order.findLine(1).getStatus());
            assertMoney(new BigDecimal("2000"), order.getReceivedAmount(),
                    "Both instalments must add up to the full line value");
            assertMoney(BigDecimal.ZERO, order.outstandingAmount(),
                    "A fully received order owes nothing further");
        }

        @Test
        @DisplayName("deliveries spread across lines and instalments converge on RECEIVED")
        void mixedSequenceOfLinesAndInstalments() {
            // The ordinary messy case: three lines, two of them arriving in instalments
            // on three different days, and the final short delivery landing last.
            PurchaseOrder order = approvedOrderWithLines(TWENTY, TEN, TWENTY);

            // Day 1: a short drop on line 1, and line 3 delivered complete.
            order.receiveAgainstLine(1, new BigDecimal("5"));
            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus());

            order.receiveAgainstLine(3, TWENTY);
            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus(),
                    "Line 3 is complete but lines 1 and 2 have not arrived");

            // Day 2: line 2 arrives complete, line 1 arrives another 10 and is
            // still 5 short of its 20.
            order.receiveAgainstLine(2, TEN);
            order.receiveAgainstLine(1, new BigDecimal("10"));
            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus(),
                    "Lines 2 and 3 are complete but line 1 is still 5 short");
            assertMoney(new BigDecimal("4500"), order.getReceivedAmount(),
                    "15 on line 1, 10 on line 2 and 20 on line 3 = 45 units at 100.00");

            // Day 3: the last 5 units land and the order closes out.
            PurchaseOrderStatus finalStatus = order.receiveAgainstLine(1, new BigDecimal("5"));

            assertEquals(PurchaseOrderStatus.RECEIVED, finalStatus,
                    "Only the final instalment completes the order");
            assertMoney(new BigDecimal("5000"), order.getReceivedAmount(),
                    "20+10+20 units at 100.00 is 5000 received");
            assertMoney(BigDecimal.ZERO, order.outstandingAmount(), "Nothing is owed");
        }

        @Test
        @DisplayName("over-receipt is refused and changes nothing")
        void overReceiptIsRefused() {
            PurchaseOrder order = approvedOrderWithLines(TEN);

            assertThrows(IllegalStateException.class,
                    () -> order.receiveAgainstLine(1, new BigDecimal("11")));

            assertMoney(BigDecimal.ZERO, order.getReceivedAmount(),
                    "A refused receipt must not have been partially applied");
            assertEquals(PurchaseOrderStatus.APPROVED, order.getStatus(),
                    "A refused receipt must not have moved the order");
        }

        @Test
        @DisplayName("receiving beyond the balance already received is refused")
        void secondInstalmentCannotExceedTheOrderedQuantity() {
            PurchaseOrder order = approvedOrderWithLines(TEN);
            order.receiveAgainstLine(1, new BigDecimal("8"));

            assertThrows(IllegalStateException.class,
                    () -> order.receiveAgainstLine(1, new BigDecimal("5")));

            assertMoney(new BigDecimal("800"), order.getReceivedAmount(),
                    "The refused instalment must leave the first one intact");
        }
    }

    @Nested
    @DisplayName("Reversing a voided receipt")
    class ReversingVoidedReceipts {

        @Test
        @DisplayName("reversing the only receipt reopens a fully received order")
        void reversingReopensAFullyReceivedOrder() {
            PurchaseOrder order = approvedOrderWithLines(TEN);
            order.receiveAgainstLine(1, TEN);
            assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());

            PurchaseOrderStatus after = order.reverseReceiptAgainstLine(1, TEN);

            assertEquals(PurchaseOrderStatus.APPROVED, after,
                    "With nothing received and nothing outstanding the order is receivable again");
            assertMoney(BigDecimal.ZERO, order.getReceivedAmount(),
                    "The reversed quantity must be handed back");
            assertTrue(order.isReceivable(),
                    "A wrongly posted receipt must leave the order able to be received again");
        }

        @Test
        @DisplayName("reversing one of several receipts leaves the rest received")
        void reversingOneLineOfManyLeavesTheOthers() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN);
            order.receiveAgainstLine(1, TEN);
            order.receiveAgainstLine(2, TEN);
            assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());

            PurchaseOrderStatus after = order.reverseReceiptAgainstLine(2, TEN);

            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, after,
                    "Line 1 is still fully received, so the order is partially received");
            assertMoney(new BigDecimal("1000"), order.getReceivedAmount(),
                    "Only line 1's value survives the reversal");
        }

        @Test
        @DisplayName("reversing more than was received is refused")
        void reversingMoreThanReceivedIsRefused() {
            PurchaseOrder order = approvedOrderWithLines(TEN);
            order.receiveAgainstLine(1, new BigDecimal("4"));

            assertThrows(IllegalStateException.class,
                    () -> order.reverseReceiptAgainstLine(1, TEN));

            assertMoney(new BigDecimal("400"), order.getReceivedAmount(),
                    "A refused reversal must leave the received quantity alone");
        }

        @Test
        @DisplayName("a closed order refuses a reversal")
        void closedOrderRefusesReversal() {
            PurchaseOrder order = approvedOrderWithLines(TEN);
            order.receiveAgainstLine(1, TEN);
            order.transitionTo(PurchaseOrderStatus.CLOSED, "finance", null);

            assertThrows(IllegalStateException.class,
                    () -> order.reverseReceiptAgainstLine(1, TEN));

            assertMoney(new BigDecimal("1000"), order.getReceivedAmount(),
                    "A settled order must not be quietly reopened by a reversal");
        }
    }

    @Nested
    @DisplayName("Closing the order")
    class Closing {

        @Test
        @DisplayName("an order still owing goods cannot be closed")
        void orderWithOutstandingGoodsCannotBeClosed() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN);
            order.receiveAgainstLine(1, TEN);

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(PurchaseOrderStatus.CLOSED, "finance", null));

            assertTrue(thrown.getMessage().contains("awaiting delivery"),
                    "The refusal should say goods are still outstanding. Was: " + thrown.getMessage());
            assertEquals(PurchaseOrderStatus.PARTIALLY_RECEIVED, order.getStatus(),
                    "A refused close must not have moved the order");
        }

        @Test
        @DisplayName("a fully received order closes")
        void fullyReceivedOrderCloses() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN);
            order.receiveAgainstLine(1, TEN);
            order.receiveAgainstLine(2, TEN);

            order.transitionTo(PurchaseOrderStatus.CLOSED, "finance", null);

            assertEquals(PurchaseOrderStatus.CLOSED, order.getStatus());
        }

        @Test
        @DisplayName("an untouched approved order cannot reach CLOSED")
        void approvedOrderCannotBeClosedDirectly() {
            PurchaseOrder order = approvedOrderWithLines(TEN);

            assertThrows(IllegalStateException.class,
                    () -> order.transitionTo(PurchaseOrderStatus.CLOSED, "finance", null),
                    "Closing means matched and settled; an order with nothing received is neither");
        }
    }

    @Nested
    @DisplayName("Ordering rules that were never in question")
    class UnchangedBehaviour {

        @Test
        @DisplayName("a draft order refuses receipt outright")
        void draftOrderRefusesReceipt() {
            PurchaseOrder order = PurchaseOrder.draft(
                    "PO-TEST-0002", ORDER_DATE, 10L, "SUP-001", "Test Supplier", "NPR", 1L);
            order.assignScope(TENANT_ID, COMPANY_ID);
            order.addLine(PurchaseOrderLine.create(1, 100L, "ITEM-1", "Item 1", "PCS", null,
                    TEN, HUNDRED, BigDecimal.ZERO, BigDecimal.ZERO));

            assertThrows(IllegalStateException.class,
                    () -> order.receiveAgainstLine(1, TEN),
                    "Goods cannot be received against an order the supplier has not been given");
        }

        @Test
        @DisplayName("outstanding lines are the worklist the receiving screen reads")
        void outstandingLinesExcludeCompletedLines() {
            PurchaseOrder order = approvedOrderWithLines(TEN, TEN, TEN);
            order.receiveAgainstLine(1, TEN);

            List<PurchaseOrderLine> outstanding = order.outstandingLines();

            assertEquals(List.of(2, 3),
                    outstanding.stream().map(PurchaseOrderLine::getLineNumber).toList(),
                    "Only undelivered lines are outstanding work");
        }

        @Test
        @DisplayName("the received amount is net of tax, matching the receipt note")
        void receivedAmountIsNetOfTaxLikeTheReceiptNote() {
            // 13% tax on 20 units at 100.00: net 2000.00, tax 260.00, total 2260.00.
            PurchaseOrder order = taxBearingOrder();
            order.receiveAgainstLine(1, TEN);

            // received_amount is the net goods value on both the order and the goods
            // receipt note, so 10 units are recognised at 1000.00 and the two
            // documents agree exactly.
            assertMoney(new BigDecimal("1000"), order.getReceivedAmount(),
                    "Half the line, at net value");

            // What is still owed is measured on the total's own basis, so it is the
            // remaining half of 2260.00 including tax. Reading it as
            // totalAmount - receivedAmount would give 1260.00, billing the supplier
            // tax on goods that have not arrived.
            assertMoney(new BigDecimal("1130.00"), order.outstandingAmount(),
                    "The remaining half of the tax-inclusive order value");
        }

        @Test
        @DisplayName("a fully received order with tax owes nothing")
        void fullyReceivedTaxBearingOrderHasNoOutstandingAmount() {
            // The defect this pins down: computed as totalAmount - receivedAmount, an
            // order that had received absolutely everything still reported 260.00
            // outstanding - exactly the tax that had arrived along with the goods.
            PurchaseOrder order = taxBearingOrder();
            order.receiveAgainstLine(1, TWENTY);

            assertEquals(PurchaseOrderStatus.RECEIVED, order.getStatus());
            assertMoney(BigDecimal.ZERO, order.outstandingAmount(),
                    "Everything ordered has arrived, so nothing is owed on the order");
        }

        @Test
        @DisplayName("freight is owed until the last item lands")
        void freightIsOwedUntilTheOrderIsComplete() {
            PurchaseOrder order = taxBearingOrder();
            order.setShippingAmount(new BigDecimal("500.00"));
            order.receiveAgainstLine(1, TEN);

            assertMoney(new BigDecimal("1630.00"), order.outstandingAmount(),
                    "1130.00 of goods still to arrive plus 500.00 of freight");

            order.receiveAgainstLine(1, TEN);

            assertMoney(BigDecimal.ZERO, order.outstandingAmount(),
                    "The last item landed, so the freight is settled too");
        }

        /** A single approved line of 20 units at 100.00 carrying 13% tax. */
        private static PurchaseOrder taxBearingOrder() {
            PurchaseOrder order = PurchaseOrder.draft(
                    "PO-TEST-0003", ORDER_DATE, 10L, "SUP-001", "Test Supplier", "NPR", 1L);
            order.assignScope(TENANT_ID, COMPANY_ID);
            order.addLine(PurchaseOrderLine.create(1, 100L, "ITEM-1", "Item 1", "PCS", null,
                    TWENTY, HUNDRED, BigDecimal.ZERO, new BigDecimal("13.00")));
            order.transitionTo(PurchaseOrderStatus.SUBMITTED, "buyer", null);
            order.transitionTo(PurchaseOrderStatus.APPROVED, "approver", null);
            return order;
        }
    }
}
