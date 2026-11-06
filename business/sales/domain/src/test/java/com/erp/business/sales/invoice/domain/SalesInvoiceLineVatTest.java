package com.erp.business.sales.invoice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesInvoiceLineVatTest {

    @Test
    void taxableExclusiveNoDiscount() {
        SalesInvoiceLine line = SalesInvoiceLine.create(
                1, 1L, "I", "Item", "PCS",
                new BigDecimal("2"), new BigDecimal("100"),
                BigDecimal.ZERO, VatType.TAXABLE, new BigDecimal("13"), null);
        assertEquals(new BigDecimal("200.0000"), line.getLineAmount());
        assertEquals(new BigDecimal("26.0000"), line.vatAmount());
    }

    @Test
    void taxableExclusiveTenPercentDiscount() {
        SalesInvoiceLine line = SalesInvoiceLine.create(
                1, 1L, "I", "Item", "PCS",
                new BigDecimal("2"), new BigDecimal("100"),
                new BigDecimal("10"), VatType.TAXABLE, new BigDecimal("13"), null);
        assertEquals(new BigDecimal("180.0000"), line.getLineAmount());
        assertEquals(new BigDecimal("23.4000"), line.vatAmount());
    }

    @Test
    void exemptNoVat() {
        SalesInvoiceLine line = SalesInvoiceLine.create(
                1, 1L, "I", "Item", "PCS",
                new BigDecimal("2"), new BigDecimal("100"),
                BigDecimal.ZERO, VatType.EXEMPT, null, null);
        assertEquals(new BigDecimal("200.0000"), line.getLineAmount());
        assertEquals(new BigDecimal("0.0000"), line.vatAmount());
    }

    @Test
    void zeroRatedNoVat() {
        SalesInvoiceLine line = SalesInvoiceLine.create(
                1, 1L, "I", "Item", "PCS",
                new BigDecimal("2"), new BigDecimal("100"),
                BigDecimal.ZERO, VatType.ZERO_RATED, null, null);
        assertEquals(new BigDecimal("200.0000"), line.getLineAmount());
        assertEquals(new BigDecimal("0.0000"), line.vatAmount());
    }

    @Test
    void exportNoVat() {
        SalesInvoiceLine line = SalesInvoiceLine.create(
                1, 1L, "I", "Item", "PCS",
                new BigDecimal("2"), new BigDecimal("100"),
                BigDecimal.ZERO, VatType.EXPORT, null, null);
        assertEquals(new BigDecimal("200.0000"), line.getLineAmount());
        assertEquals(new BigDecimal("0.0000"), line.vatAmount());
    }
}
