package com.erp.business.pos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PosTransactionHospitalityTest {

    @Test
    @DisplayName("Hospitality billing recalculates taxable amount and VAT based on (Subtotal - Discount) + Service Charge")
    void testHospitalityBillingCalculation() {
        PosTransaction tx = PosTransaction.open(
                1L, 1L, 1L, "WH-1", "Main Warehouse",
                "cashier-1", "Cashier John",
                LocalDate.now(), "2081-08-06",
                1L, 1L, null, null, "Walk-in Customer", null,
                null, "Hospitality Dine-in"
        );
        tx.assignScope(1L, 1L);
        tx.setId(100L);
        tx.setTaxInvoice(true);

        // Add 2 lines: Momo (Rs. 600) and Beverage (Rs. 400) = Rs. 1000 subtotal
        PosTransactionLine line1 = PosTransactionLine.of(
                1, 101L, "MOMO", "Chicken Steam Momo", null, "PLT",
                new BigDecimal("2.000000"), new BigDecimal("300.0000"), BigDecimal.ZERO,
                PosVatType.TAXABLE, new BigDecimal("13.00"), null
        );
        PosTransactionLine line2 = PosTransactionLine.of(
                2, 102L, "COKE", "Coke 500ml", null, "BOT",
                new BigDecimal("4.000000"), new BigDecimal("100.0000"), BigDecimal.ZERO,
                PosVatType.TAXABLE, new BigDecimal("13.00"), null
        );
        tx.addLine(line1);
        tx.addLine(line2);

        // Subtotal = 1000
        assertEquals(new BigDecimal("1000.0000"), tx.getSubtotalAmount());

        // Apply Rs. 100 discount and 10% Service Charge (Rs. 90)
        // Net Food = 1000 - 100 = 900
        // Service Charge = 90
        // Taxable Base = 900 + 90 = 990
        // 13% VAT = 990 * 0.13 = 128.70
        // Grand Total = 990 + 128.70 = 1118.70
        tx.applyHospitalityBilling(new BigDecimal("100.00"), "Guest VIP Discount", new BigDecimal("90.00"));

        assertEquals(new BigDecimal("100.0000"), tx.getDiscountAmount());
        assertEquals("Guest VIP Discount", tx.getDiscountReason());
        assertEquals(new BigDecimal("90.0000"), tx.getServiceChargeAmount());
        assertEquals(new BigDecimal("990.0000"), tx.getTaxableAmount());
        assertEquals(new BigDecimal("128.7000"), tx.getVatAmount());
        assertEquals(new BigDecimal("1118.7000"), tx.getTotalAmount());

        // Test room charge completion
        tx.setHospitalityDetails("Room 304", "Ramesh Thapa", null);
        tx.complete("POS-2026-0001", PosPaymentMethod.ROOM_CHARGE, tx.getTotalAmount(), "Folio #304", "cashier-1");

        assertEquals(PosTransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(PosPaymentMethod.ROOM_CHARGE, tx.getPaymentMethod());
        assertEquals("Room 304", tx.getRoomNumber());
        assertEquals("Ramesh Thapa", tx.getGuestName());
        assertEquals("POS-2026-0001", tx.getTransactionNumber());
    }

    @Test
    @DisplayName("Split payment records split tender details and completes cleanly")
    void testSplitPaymentCompletion() {
        PosTransaction tx = PosTransaction.open(
                1L, 1L, 1L, "WH-1", "Main Warehouse",
                "cashier-1", "Cashier John",
                LocalDate.now(), "2081-08-06",
                1L, 1L, null, null, "Walk-in Customer", null,
                null, "Split Bill"
        );
        tx.assignScope(1L, 1L);
        tx.setId(101L);
        tx.setTaxInvoice(false); // Non-VAT cash bill

        PosTransactionLine line1 = PosTransactionLine.of(
                1, 101L, "MOMO", "Veg Momo", null, "PLT",
                new BigDecimal("1.000000"), new BigDecimal("200.0000"), BigDecimal.ZERO,
                PosVatType.EXEMPT, BigDecimal.ZERO, null
        );
        tx.addLine(line1);

        tx.setHospitalityDetails(null, null, "Cash: Rs. 100, Fonepay: Rs. 100");
        tx.complete("POS-2026-0002", PosPaymentMethod.SPLIT, tx.getTotalAmount(), "Cash + Fonepay", "cashier-1");

        assertEquals(PosTransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(PosPaymentMethod.SPLIT, tx.getPaymentMethod());
        assertEquals("Cash: Rs. 100, Fonepay: Rs. 100", tx.getSplitPaymentDetails());
    }

    @Test
    @DisplayName("Takeaway order records token number and customer phone")
    void testTakeawayDetails() {
        PosTransaction tx = PosTransaction.open(
                1L, 1L, 1L, "WH-1", "Main Warehouse",
                "cashier-1", "Cashier John",
                LocalDate.now(), "2081-08-06",
                1L, 1L, null, null, "Quick Takeaway Guest", null,
                null, "Takeaway Order"
        );
        tx.assignScope(1L, 1L);
        tx.setId(102L);

        PosTransactionLine line = PosTransactionLine.of(
                1, 101L, "PAKODA", "Paneer Pakoda", null, "PLT",
                new BigDecimal("2.000000"), new BigDecimal("150.0000"), BigDecimal.ZERO,
                PosVatType.TAXABLE, new BigDecimal("13.00"), null
        );
        tx.addLine(line);

        tx.setDeliveryAndTokenDetails("TAKEAWAY", "TK-042", null, null, "9841234567", BigDecimal.ZERO);

        assertEquals("TAKEAWAY", tx.getOrderType());
        assertEquals("TK-042", tx.getTokenNumber());
        assertEquals("9841234567", tx.getCustomerPhone());
        assertNull(tx.getDeliveryPartner());
        assertNull(tx.getDeliveryAddress());
        assertEquals(new BigDecimal("0.0000"), tx.getDeliveryCharge());
    }

    @Test
    @DisplayName("Delivery order adds delivery charge to total amount and records rider platform details")
    void testDeliveryOrderWithCharge() {
        PosTransaction tx = PosTransaction.open(
                1L, 1L, 1L, "WH-1", "Main Warehouse",
                "cashier-1", "Cashier John",
                LocalDate.now(), "2081-08-06",
                1L, 1L, null, null, "Delivery Customer", null,
                null, "Foodmandu Dispatch"
        );
        tx.assignScope(1L, 1L);
        tx.setId(103L);
        tx.setTaxInvoice(true);

        PosTransactionLine line = PosTransactionLine.of(
                1, 101L, "MOMO", "Buff C-Momo", null, "PLT",
                new BigDecimal("2.000000"), new BigDecimal("250.0000"), BigDecimal.ZERO,
                PosVatType.TAXABLE, new BigDecimal("13.00"), null
        );
        tx.addLine(line);

        // Subtotal = 500. VAT 13% = 65. Food total = 565.
        // Delivery Charge = 50. Grand Total = 565 + 50 = 615.
        tx.setDeliveryAndTokenDetails("DELIVERY", "TK-099", "Foodmandu", "Sanepa, Ward 2", "9801234567", new BigDecimal("50.00"));

        assertEquals("DELIVERY", tx.getOrderType());
        assertEquals("TK-099", tx.getTokenNumber());
        assertEquals("Foodmandu", tx.getDeliveryPartner());
        assertEquals("Sanepa, Ward 2", tx.getDeliveryAddress());
        assertEquals("9801234567", tx.getCustomerPhone());
        assertEquals(new BigDecimal("50.0000"), tx.getDeliveryCharge());
        assertEquals(new BigDecimal("500.0000"), tx.getTaxableAmount());
        assertEquals(new BigDecimal("65.0000"), tx.getVatAmount());
        assertEquals(new BigDecimal("615.0000"), tx.getTotalAmount());
    }
}
