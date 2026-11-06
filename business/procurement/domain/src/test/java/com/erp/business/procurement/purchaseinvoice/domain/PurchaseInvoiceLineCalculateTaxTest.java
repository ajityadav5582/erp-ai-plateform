package com.erp.business.procurement.purchaseinvoice.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseInvoiceLineCalculateTaxTest {

    @Test
    void taxableExclusiveNoDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("0"));
        assertEquals(new BigDecimal("200.0000"), line.getTaxableAmount());
        assertEquals(new BigDecimal("26.0000"), line.getTaxAmount());
    }

    @Test
    void taxableExclusiveTenPercentDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("10"));
        assertEquals(new BigDecimal("180.0000"), line.getTaxableAmount());
        assertEquals(new BigDecimal("23.4000"), line.getTaxAmount());
    }

    @Test
    void taxableExclusiveHundredPercentDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("100"));
        assertEquals(new BigDecimal("0.0000"), line.getTaxableAmount());
        assertEquals(new BigDecimal("0.0000"), line.getTaxAmount());
    }

    @Test
    void taxableInclusiveTenPercentDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("1")).unitPrice(new BigDecimal("113")).lineAmount(new BigDecimal("113")).build();
        line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("113"), new BigDecimal("1"), new BigDecimal("13"), true, new BigDecimal("10"));
        assertEquals(new BigDecimal("90.0000"), line.getTaxableAmount());
        assertEquals(new BigDecimal("11.7000"), line.getTaxAmount());
    }

    @Test
    void exemptWithDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.EXEMPT, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("10"));
        assertEquals(PurchaseTaxCategory.EXEMPT, line.getTaxCategory());
        assertEquals(new BigDecimal("0.0000"), line.getTaxAmount());
    }

    @Test
    void zeroRatedWithDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.ZERO_RATED, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("10"));
        assertEquals(PurchaseTaxCategory.ZERO_RATED, line.getTaxCategory());
        assertEquals(new BigDecimal("0.0000"), line.getTaxAmount());
    }

    @Test
    void nonTaxableWithDiscount() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        line.calculateTax(PurchaseTaxCategory.NON_TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("10"));
        assertEquals(PurchaseTaxCategory.NON_TAXABLE, line.getTaxCategory());
        assertEquals(new BigDecimal("0.0000"), line.getTaxAmount());
    }

    @Test
    void invalidDiscountRejected() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        assertThrows(IllegalArgumentException.class, () -> line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> line.calculateTax(PurchaseTaxCategory.TAXABLE, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("101")));
    }

    @Test
    void unclassifiedRejected() {
        PurchaseInvoiceLine line = PurchaseInvoiceLine.builder().lineNumber(1).itemId(1L).itemCode("I").itemName("Item").uomCode("PCS").quantity(new BigDecimal("2")).unitPrice(new BigDecimal("100")).lineAmount(new BigDecimal("200")).build();
        assertThrows(IllegalArgumentException.class, () -> line.calculateTax(PurchaseTaxCategory.UNCLASSIFIED, new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("13"), false, new BigDecimal("0")));
    }
}
