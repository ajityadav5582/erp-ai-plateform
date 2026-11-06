package com.erp.business.procurement.purchaseinvoice.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PurchaseInvoiceSupplierVatSnapshotTest {

    @Test
    void setSupplierVatSnapshotStoresValues() {
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .invoiceNumber("INV-1")
                .invoiceDate(java.time.LocalDate.now())
                .supplierId(1L)
                .supplierCode("SUP")
                .supplierName("Supplier")
                .currency("NPR")
                .status(PurchaseInvoiceStatus.APPROVED)
                .lines(new java.util.ArrayList<>())
                .build();

        invoice.setSupplierVatSnapshot("VAT_REGISTERED", "VAT-123", "PAN-456");

        assertEquals("VAT_REGISTERED", invoice.getSupplierVatRegistrationStatus());
        assertEquals("VAT-123", invoice.getSupplierVatRegistrationNumber());
        assertEquals("PAN-456", invoice.getSupplierTaxId());
    }

    @Test
    void panOnlySupplierSnapshot() {
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .invoiceNumber("INV-2")
                .invoiceDate(java.time.LocalDate.now())
                .supplierId(2L)
                .supplierCode("SUP2")
                .supplierName("Supplier2")
                .currency("NPR")
                .status(PurchaseInvoiceStatus.APPROVED)
                .lines(new java.util.ArrayList<>())
                .build();

        invoice.setSupplierVatSnapshot("PAN_ONLY", null, "PAN-789");

        assertEquals("PAN_ONLY", invoice.getSupplierVatRegistrationStatus());
        assertNull(invoice.getSupplierVatRegistrationNumber());
        assertEquals("PAN-789", invoice.getSupplierTaxId());
    }

    @Test
    void nullSnapshotFieldsRemainReadable() {
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .invoiceNumber("INV-3")
                .invoiceDate(java.time.LocalDate.now())
                .supplierId(3L)
                .supplierCode("SUP3")
                .supplierName("Supplier3")
                .currency("NPR")
                .status(PurchaseInvoiceStatus.APPROVED)
                .lines(new java.util.ArrayList<>())
                .build();

        assertNull(invoice.getSupplierVatRegistrationStatus());
        assertNull(invoice.getSupplierVatRegistrationNumber());
        assertNull(invoice.getSupplierTaxId());
    }
}
