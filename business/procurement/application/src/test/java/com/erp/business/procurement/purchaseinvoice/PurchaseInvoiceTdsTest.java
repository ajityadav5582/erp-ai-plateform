package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.purchaseinvoice.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseInvoiceTdsTest {

    @Test
    @DisplayName("Invoice with 1.5% Nepal TDS calculates net payable and settles at net amount")
    void invoiceWithOneAndHalfPercentTds() {
        PurchaseInvoice invoice = PurchaseInvoice.direct(
                "INV-TDS-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                10L,
                "SUP-10",
                "Apex Supplies Pvt Ltd",
                "NPR",
                1L,
                1L,
                "Office equipment procurement"
        );
        invoice.assignScope(1L, 10L);

        // Subtotal = 100,000, VAT 13% = 13,000 -> Total Gross = 113,000
        invoice.configureTds(true, new BigDecimal("1.50"));

        PurchaseInvoiceLine line = PurchaseInvoiceLine.direct(
                1,
                101L,
                "ITEM-101",
                "Laptop",
                "PCS",
                new BigDecimal("1"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                new BigDecimal("13.00"),
                "High performance laptop"
        );
        invoice.addLine(line);

        assertThat(invoice.getSubtotalAmount()).isEqualByComparingTo("100000.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("13000.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("113000.00");

        // 1.5% TDS on subtotal (100,000 * 1.5% = 1,500)
        assertThat(invoice.getTdsAmount()).isEqualByComparingTo("1500.00");
        // Net payable = 113,000 - 1,500 = 111,500
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo("111500.00");
        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("111500.00");
        assertThat(invoice.getStatus()).isEqualTo(PurchaseInvoiceStatus.APPROVED);

        // Pay exact net payable amount of 111,500
        invoice.recordPayment(new BigDecimal("111500.00"));

        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("111500.00");
        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getStatus()).isEqualTo(PurchaseInvoiceStatus.PAID);

        // Attempting to pay beyond net payable must fail
        assertThatThrownBy(() -> invoice.recordPayment(new BigDecimal("100.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("would exceed the balance of 0.0000");
    }

    @Test
    @DisplayName("Invoice without TDS defaults net payable to total amount")
    void invoiceWithoutTdsDefaultsToTotalAmount() {
        PurchaseInvoice invoice = PurchaseInvoice.direct(
                "INV-NO-TDS-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                10L,
                "SUP-10",
                "Apex Supplies Pvt Ltd",
                "NPR",
                1L,
                1L,
                "Office supplies"
        );
        invoice.assignScope(1L, 10L);
        invoice.configureTds(false, null);

        PurchaseInvoiceLine line = PurchaseInvoiceLine.direct(
                1,
                102L,
                "ITEM-102",
                "Desk",
                "PCS",
                new BigDecimal("2"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                new BigDecimal("13.00"),
                "Office desk"
        );
        invoice.addLine(line);

        assertThat(invoice.getSubtotalAmount()).isEqualByComparingTo("10000.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("1300.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("11300.00");
        assertThat(invoice.getTdsAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo("11300.00");
        assertThat(invoice.getBalanceAmount()).isEqualByComparingTo("11300.00");
    }
}
