package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.procurement.goodsreceipt.GoodsReceiptService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.dto.GoodsReceiptResponse;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import com.erp.business.procurement.purchaseinvoice.dto.FastMatchPoRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseInvoiceFastMatchServiceTest {

    @Test
    void fastMatchPurchaseOrder_recordsGrnAndCreatesInvoice() {
        PurchaseInvoiceRepository invoiceRepo = Mockito.mock(PurchaseInvoiceRepository.class);
        GoodsReceiptRepository receiptRepo = Mockito.mock(GoodsReceiptRepository.class);
        ItemService itemService = Mockito.mock(ItemService.class);
        SupplierService supplierService = Mockito.mock(SupplierService.class);
        TaxRateService taxRateService = Mockito.mock(TaxRateService.class);
        GoodsReceiptService goodsReceiptService = Mockito.mock(GoodsReceiptService.class);

        PurchaseInvoiceServiceImpl service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService, goodsReceiptService);

        Long tenantId = 1L;
        Long companyId = 10L;
        Long poId = 100L;
        Long grnId = 200L;
        Long grnLineId = 201L;

        GoodsReceiptResponse.Line grnLine = new GoodsReceiptResponse.Line(
                grnLineId, 1, 101L, 1L, "ITEM-01", "Widget", "PCS", 12L,
                BigDecimal.valueOf(10), BigDecimal.valueOf(150), BigDecimal.valueOf(1500),
                5L, "Arrived"
        );

        GoodsReceiptResponse mockGrnResponse = new GoodsReceiptResponse(
                grnId, "GRN-000200", tenantId, companyId, 1L, 1L,
                poId, "PO-000100", LocalDate.now(), 50L, "SUP-50", "Supplier 50",
                "NPR", GoodsReceiptStatus.POSTED, null, null, BigDecimal.valueOf(1500),
                5L, "WH-1", "Main WH", "CH-123", "Fast-match test", null, "user1",
                null, null, null, List.of(grnLine)
        );

        when(goodsReceiptService.record(eq(tenantId), eq(companyId), any(), eq("user1")))
                .thenReturn(mockGrnResponse);

        GoodsReceipt receiptEntity = Mockito.mock(GoodsReceipt.class);
        when(receiptEntity.getId()).thenReturn(grnId);
        when(receiptEntity.getGrnNumber()).thenReturn("GRN-000200");
        when(receiptEntity.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receiptEntity.getSupplierId()).thenReturn(50L);
        when(receiptEntity.getSupplierCode()).thenReturn("SUP-50");
        when(receiptEntity.getSupplierName()).thenReturn("Supplier 50");
        when(receiptEntity.getCurrency()).thenReturn("NPR");
        when(receiptEntity.getPurchaseOrderId()).thenReturn(poId);
        when(receiptEntity.getPoNumber()).thenReturn("PO-000100");

        GoodsReceiptLine receiptLineEntity = Mockito.mock(GoodsReceiptLine.class);
        when(receiptLineEntity.getId()).thenReturn(grnLineId);
        when(receiptLineEntity.getItemId()).thenReturn(1L);
        when(receiptLineEntity.getReceivedQuantity()).thenReturn(BigDecimal.valueOf(10));
        when(receiptLineEntity.getUnitPrice()).thenReturn(BigDecimal.valueOf(150));
        when(receiptLineEntity.getTaxCategory()).thenReturn(PurchaseTaxCategory.TAXABLE);
        when(receiptEntity.getLines()).thenReturn(List.of(receiptLineEntity));

        when(receiptRepo.findByIdForUpdate(tenantId, companyId, grnId))
                .thenReturn(Optional.of(receiptEntity));
        when(invoiceRepo.findByTenantIdAndCompanyIdAndGoodsReceiptId(tenantId, companyId, grnId))
                .thenReturn(Optional.empty());
        when(invoiceRepo.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(tenantId, companyId, 50L, "INV-FAST-1"))
                .thenReturn(false);
        when(invoiceRepo.saveAndFlush(any(PurchaseInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxRateService.TaxRate taxRate = new TaxRateService.TaxRate(
                1L, "VAT", "VAT", BigDecimal.valueOf(13), "TAXABLE", LocalDate.now().minusDays(1), null);
        when(taxRateService.findActive(eq(tenantId), eq(companyId), eq("VAT"), any()))
                .thenReturn(taxRate);

        FastMatchPoRequest request = new FastMatchPoRequest(
                poId, "INV-FAST-1", LocalDate.now(), LocalDate.now().plusDays(30),
                5L, "CH-123", "Simultaneous arrival"
        );

        PurchaseInvoiceResponse invoice = service.fastMatchPurchaseOrder(tenantId, companyId, request, "user1");

        assertNotNull(invoice);
        assertEquals("INV-FAST-1", invoice.invoiceNumber());
        assertEquals(grnId, invoice.goodsReceiptId());
        assertEquals("GRN-000200", invoice.grnNumber());
        assertEquals(poId, invoice.purchaseOrderId());
        assertEquals("PO-000100", invoice.poNumber());

        verify(goodsReceiptService).record(eq(tenantId), eq(companyId), any(), eq("user1"));
        verify(invoiceRepo).saveAndFlush(any(PurchaseInvoice.class));
    }
}

