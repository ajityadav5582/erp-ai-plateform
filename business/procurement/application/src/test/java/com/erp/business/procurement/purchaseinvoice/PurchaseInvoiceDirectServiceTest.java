package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceType;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.dto.PurchaseInvoiceResponse;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class PurchaseInvoiceDirectServiceTest {

    @Test
    void createDirectServiceInvoiceSuccessfully() {
        PurchaseInvoiceRepository invoiceRepo = Mockito.mock(PurchaseInvoiceRepository.class);
        GoodsReceiptRepository receiptRepo = Mockito.mock(GoodsReceiptRepository.class);
        ItemService itemService = Mockito.mock(ItemService.class);
        SupplierService supplierService = Mockito.mock(SupplierService.class);
        TaxRateService taxRateService = Mockito.mock(TaxRateService.class);

        // Mock Supplier
        SupplierService.SupplierResponse supplier = Mockito.mock(SupplierService.SupplierResponse.class);
        when(supplier.getId()).thenReturn(5L);
        when(supplier.getCode()).thenReturn("SUP-LEGAL");
        when(supplier.getName()).thenReturn("Apex Legal Advisory");
        when(supplier.getVatRegistrationStatus()).thenReturn("VAT_REGISTERED");
        when(supplier.getVatRegistrationNumber()).thenReturn("VAT-998877");
        when(supplier.getTaxNumber()).thenReturn("PAN-112233");
        when(supplierService.findById(5L)).thenReturn(Optional.of(supplier));

        // Mock Service Item (Legal consulting)
        ItemService.ItemResponse item = Mockito.mock(ItemService.ItemResponse.class);
        when(item.getId()).thenReturn(20L);
        when(item.getSku()).thenReturn("SRV-LEGAL-01");
        when(item.getName()).thenReturn("Legal Advisory & Compliance Retainer");
        when(item.getUnitCode()).thenReturn("MONTH");
        when(item.getTaxabilityType()).thenReturn("TAXABLE");
        when(item.getVatRate()).thenReturn(new BigDecimal("13.00"));
        when(item.getIsVatInclusive()).thenReturn(false);
        when(itemService.findById(20L)).thenReturn(Optional.of(item));

        // Mock VAT Tax Rate
        TaxRateService.TaxRate taxRate = new TaxRateService.TaxRate(
                1L, "VAT", "VAT", new BigDecimal("13.00"), "TAXABLE",
                LocalDate.now().minusDays(1), null);
        when(taxRateService.findActive(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq("VAT"), Mockito.any())).thenReturn(taxRate);

        when(invoiceRepo.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(1L, 1L, 5L, "EXP-INV-101"))
                .thenReturn(false);
        when(invoiceRepo.saveAndFlush(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        com.erp.business.procurement.goodsreceipt.GoodsReceiptService goodsReceiptService = Mockito.mock(com.erp.business.procurement.goodsreceipt.GoodsReceiptService.class);
        PurchaseInvoiceServiceImpl service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService, goodsReceiptService);

        CreateInvoiceRequest.Line line = new CreateInvoiceRequest.Line(
                null, 20L, "SRV-LEGAL-01", "Legal Advisory & Compliance Retainer", "MONTH",
                new BigDecimal("1.000000"), new BigDecimal("50000.0000"), BigDecimal.ZERO,
                new BigDecimal("13.00"), "Monthly legal consultation fee", PurchaseTaxCategory.TAXABLE);

        CreateInvoiceRequest request = new CreateInvoiceRequest(
                null, PurchaseInvoiceType.DIRECT, 5L, 1L, 1L, "NPR",
                "EXP-INV-101", LocalDate.now(), LocalDate.now().plusDays(30),
                List.of(line), "Direct legal service bill");

        PurchaseInvoiceResponse response = service.create(1L, 1L, request);

        assertNotNull(response);
        assertEquals("EXP-INV-101", response.invoiceNumber());
        assertEquals(PurchaseInvoiceType.DIRECT, response.invoiceType());
        assertNull(response.goodsReceiptId());
        assertNull(response.purchaseOrderId());

        ArgumentCaptor<PurchaseInvoice> captor = ArgumentCaptor.forClass(PurchaseInvoice.class);
        Mockito.verify(invoiceRepo).saveAndFlush(captor.capture());
        PurchaseInvoice saved = captor.getValue();

        assertEquals(PurchaseInvoiceType.DIRECT, saved.getInvoiceType());
        assertNull(saved.getGoodsReceiptId());
        assertEquals(5L, saved.getSupplierId());
        assertEquals("VAT_REGISTERED", saved.getSupplierVatRegistrationStatus());
        assertEquals("VAT-998877", saved.getSupplierVatRegistrationNumber());
        assertEquals("PAN-112233", saved.getSupplierTaxId());
        assertEquals(new BigDecimal("50000.0000"), saved.getSubtotalAmount());
        assertEquals(new BigDecimal("6500.0000"), saved.getTaxAmount());
        assertEquals(new BigDecimal("56500.0000"), saved.getTotalAmount());
    }
}

