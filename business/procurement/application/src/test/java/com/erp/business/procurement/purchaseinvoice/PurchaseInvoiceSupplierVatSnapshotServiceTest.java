package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class PurchaseInvoiceSupplierVatSnapshotServiceTest {

    @Test
    void vatRegisteredSupplierSnapshot() {
        PurchaseInvoiceRepository invoiceRepo = Mockito.mock(PurchaseInvoiceRepository.class);
        GoodsReceiptRepository receiptRepo = Mockito.mock(GoodsReceiptRepository.class);
        ItemService itemService = Mockito.mock(ItemService.class);
        SupplierService supplierService = Mockito.mock(SupplierService.class);
        TaxRateService taxRateService = Mockito.mock(TaxRateService.class);

        // Mock supplier
        SupplierService.SupplierResponse supplier = Mockito.mock(SupplierService.SupplierResponse.class);
        when(supplier.getVatRegistrationStatus()).thenReturn("VAT_REGISTERED");
        when(supplier.getVatRegistrationNumber()).thenReturn("VAT-123");
        when(supplier.getTaxNumber()).thenReturn("PAN-456");
        when(supplierService.findById(1L)).thenReturn(Optional.of(supplier));

        // Mock goods receipt
        GoodsReceipt receipt = Mockito.mock(GoodsReceipt.class);
        when(receipt.getId()).thenReturn(10L);
        when(receipt.getGrnNumber()).thenReturn("GRN-1");
        when(receipt.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receipt.getSupplierId()).thenReturn(1L);
        when(receipt.getSupplierCode()).thenReturn("SUP");
        when(receipt.getSupplierName()).thenReturn("Supplier");
        when(receipt.getCurrency()).thenReturn("NPR");
        when(receipt.getPurchaseOrderId()).thenReturn(1L);
        when(receipt.getPoNumber()).thenReturn("PO-1");
        when(receipt.getFiscalYearId()).thenReturn(1L);
        when(receipt.getBranchId()).thenReturn(1L);

        GoodsReceiptLine receiptLine = Mockito.mock(GoodsReceiptLine.class);
        when(receiptLine.getId()).thenReturn(100L);
        when(receiptLine.getItemId()).thenReturn(1L);
        when(receiptLine.getReceivedQuantity()).thenReturn(new BigDecimal("2"));
        when(receiptLine.getUnitPrice()).thenReturn(new BigDecimal("100"));
        when(receipt.getLines()).thenReturn(List.of(receiptLine));
        when(receiptLine.getTaxCategory()).thenReturn(
                com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE);

        when(receiptRepo.findByIdForUpdate(1L, 1L, 10L)).thenReturn(Optional.of(receipt));
        when(invoiceRepo.findByTenantIdAndCompanyIdAndGoodsReceiptId(1L, 1L, 10L))
                .thenReturn(Optional.empty());
        when(invoiceRepo.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(1L, 1L, 1L, "INV-001"))
                .thenReturn(false);
        when(invoiceRepo.saveAndFlush(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        ItemService.ItemResponse item = Mockito.mock(ItemService.ItemResponse.class);
        when(item.getTaxabilityType()).thenReturn("TAXABLE");
        when(item.getIsVatInclusive()).thenReturn(false);
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        TaxRateService.TaxRate taxRate = new TaxRateService.TaxRate(
                1L, "VAT", "VAT", new BigDecimal("13"), "TAXABLE",
                LocalDate.now().minusDays(1), null);
        when(taxRateService.findActive(Mockito.eq(1L), Mockito.eq(1L), Mockito.eq("VAT"), Mockito.any())).thenReturn(taxRate);

        PurchaseInvoiceServiceImpl service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService);

        CreateInvoiceRequest.Line line = new CreateInvoiceRequest.Line(
                100L, null, null, null, null, null,
                PurchaseTaxCategory.TAXABLE);
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                10L, "INV-001", LocalDate.now(), null, List.of(line), null);

        service.create(1L, 1L, request);

        ArgumentCaptor<PurchaseInvoice> captor = ArgumentCaptor.forClass(PurchaseInvoice.class);
        Mockito.verify(invoiceRepo).saveAndFlush(captor.capture());
        PurchaseInvoice saved = captor.getValue();

        assertEquals("VAT_REGISTERED", saved.getSupplierVatRegistrationStatus());
        assertEquals("VAT-123", saved.getSupplierVatRegistrationNumber());
        assertEquals("PAN-456", saved.getSupplierTaxId());
    }

    @Test
    void panOnlySupplierSnapshot() {
        PurchaseInvoiceRepository invoiceRepo = Mockito.mock(PurchaseInvoiceRepository.class);
        GoodsReceiptRepository receiptRepo = Mockito.mock(GoodsReceiptRepository.class);
        ItemService itemService = Mockito.mock(ItemService.class);
        SupplierService supplierService = Mockito.mock(SupplierService.class);
        TaxRateService taxRateService = Mockito.mock(TaxRateService.class);

        SupplierService.SupplierResponse supplier = Mockito.mock(SupplierService.SupplierResponse.class);
        when(supplier.getVatRegistrationStatus()).thenReturn("PAN_ONLY");
        when(supplier.getVatRegistrationNumber()).thenReturn(null);
        when(supplier.getTaxNumber()).thenReturn("PAN-789");
        when(supplierService.findById(2L)).thenReturn(Optional.of(supplier));

        GoodsReceipt receipt = Mockito.mock(GoodsReceipt.class);
        when(receipt.getId()).thenReturn(20L);
        when(receipt.getGrnNumber()).thenReturn("GRN-2");
        when(receipt.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receipt.getSupplierId()).thenReturn(2L);
        when(receipt.getSupplierCode()).thenReturn("SUP2");
        when(receipt.getSupplierName()).thenReturn("Supplier2");
        when(receipt.getCurrency()).thenReturn("NPR");
        when(receipt.getPurchaseOrderId()).thenReturn(2L);
        when(receipt.getPoNumber()).thenReturn("PO-2");
        when(receipt.getFiscalYearId()).thenReturn(1L);
        when(receipt.getBranchId()).thenReturn(1L);

        GoodsReceiptLine receiptLine = Mockito.mock(GoodsReceiptLine.class);
        when(receiptLine.getId()).thenReturn(200L);
        when(receiptLine.getItemId()).thenReturn(2L);
        when(receiptLine.getReceivedQuantity()).thenReturn(new BigDecimal("1"));
        when(receiptLine.getUnitPrice()).thenReturn(new BigDecimal("50"));
        when(receipt.getLines()).thenReturn(List.of(receiptLine));
        when(receiptLine.getTaxCategory()).thenReturn(
                com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.NON_TAXABLE);

        when(receiptRepo.findByIdForUpdate(1L, 1L, 20L)).thenReturn(Optional.of(receipt));
        when(invoiceRepo.findByTenantIdAndCompanyIdAndGoodsReceiptId(1L, 1L, 20L))
                .thenReturn(Optional.empty());
        when(invoiceRepo.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(1L, 1L, 2L, "INV-002"))
                .thenReturn(false);
        when(invoiceRepo.saveAndFlush(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseInvoiceServiceImpl service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService);

        CreateInvoiceRequest.Line line = new CreateInvoiceRequest.Line(
                200L, null, null, null, null, null,
                PurchaseTaxCategory.NON_TAXABLE);
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                20L, "INV-002", LocalDate.now(), null, List.of(line), null);

        service.create(1L, 1L, request);

        ArgumentCaptor<PurchaseInvoice> captor = ArgumentCaptor.forClass(PurchaseInvoice.class);
        Mockito.verify(invoiceRepo).saveAndFlush(captor.capture());
        PurchaseInvoice saved = captor.getValue();

        assertEquals("PAN_ONLY", saved.getSupplierVatRegistrationStatus());
        assertNull(saved.getSupplierVatRegistrationNumber());
        assertEquals("PAN-789", saved.getSupplierTaxId());
    }

    @Test
    void supplierLookupFailureDoesNotCreateIncorrectSnapshot() {
        PurchaseInvoiceRepository invoiceRepo = Mockito.mock(PurchaseInvoiceRepository.class);
        GoodsReceiptRepository receiptRepo = Mockito.mock(GoodsReceiptRepository.class);
        ItemService itemService = Mockito.mock(ItemService.class);
        SupplierService supplierService = Mockito.mock(SupplierService.class);
        TaxRateService taxRateService = Mockito.mock(TaxRateService.class);

        // Supplier not found
        when(supplierService.findById(3L)).thenReturn(Optional.empty());

        GoodsReceipt receipt = Mockito.mock(GoodsReceipt.class);
        when(receipt.getId()).thenReturn(30L);
        when(receipt.getGrnNumber()).thenReturn("GRN-3");
        when(receipt.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receipt.getSupplierId()).thenReturn(3L);
        when(receipt.getSupplierCode()).thenReturn("SUP3");
        when(receipt.getSupplierName()).thenReturn("Supplier3");
        when(receipt.getCurrency()).thenReturn("NPR");
        when(receipt.getPurchaseOrderId()).thenReturn(3L);
        when(receipt.getPoNumber()).thenReturn("PO-3");
        when(receipt.getFiscalYearId()).thenReturn(1L);
        when(receipt.getBranchId()).thenReturn(1L);

        GoodsReceiptLine receiptLine = Mockito.mock(GoodsReceiptLine.class);
        when(receiptLine.getId()).thenReturn(300L);
        when(receiptLine.getItemId()).thenReturn(3L);
        when(receiptLine.getReceivedQuantity()).thenReturn(new BigDecimal("1"));
        when(receiptLine.getUnitPrice()).thenReturn(new BigDecimal("10"));
        when(receipt.getLines()).thenReturn(List.of(receiptLine));
        when(receiptLine.getTaxCategory()).thenReturn(
                com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.EXEMPT);

        when(receiptRepo.findByIdForUpdate(1L, 1L, 30L)).thenReturn(Optional.of(receipt));
        when(invoiceRepo.findByTenantIdAndCompanyIdAndGoodsReceiptId(1L, 1L, 30L))
                .thenReturn(Optional.empty());
        when(invoiceRepo.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(1L, 1L, 3L, "INV-003"))
                .thenReturn(false);
        when(invoiceRepo.saveAndFlush(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseInvoiceServiceImpl service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService);

        CreateInvoiceRequest.Line line = new CreateInvoiceRequest.Line(
                300L, null, null, null, null, null,
                PurchaseTaxCategory.EXEMPT);
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                30L, "INV-003", LocalDate.now(), null, List.of(line), null);

        service.create(1L, 1L, request);

        ArgumentCaptor<PurchaseInvoice> captor = ArgumentCaptor.forClass(PurchaseInvoice.class);
        Mockito.verify(invoiceRepo).saveAndFlush(captor.capture());
        PurchaseInvoice saved = captor.getValue();

        // Supplier lookup failed: snapshot fields remain null (no incorrect data)
        assertNull(saved.getSupplierVatRegistrationStatus());
        assertNull(saved.getSupplierVatRegistrationNumber());
        assertNull(saved.getSupplierTaxId());
    }
}
