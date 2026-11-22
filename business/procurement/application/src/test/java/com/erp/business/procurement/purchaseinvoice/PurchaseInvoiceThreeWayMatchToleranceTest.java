package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.procurement.goodsreceipt.GoodsReceiptService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceType;
import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.platform.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseInvoiceThreeWayMatchToleranceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long GRN_ID = 200L;
    private static final Long GRN_LINE_ID = 201L;

    @Mock private PurchaseInvoiceRepository invoiceRepo;
    @Mock private GoodsReceiptRepository receiptRepo;
    @Mock private ItemService itemService;
    @Mock private SupplierService supplierService;
    @Mock private TaxRateService taxRateService;
    @Mock private GoodsReceiptService goodsReceiptService;

    private PurchaseInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseInvoiceServiceImpl(
                invoiceRepo, receiptRepo, itemService, supplierService, taxRateService, goodsReceiptService);
    }

    @Test
    @DisplayName("3-way match fails when billed quantity exceeds received quantity")
    void billedQuantityExceedingReceivedQuantity_failsThreeWayMatch() {
        GoodsReceipt receipt = mock(GoodsReceipt.class);
        when(receipt.getId()).thenReturn(GRN_ID);
        when(receipt.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receipt.getSupplierId()).thenReturn(50L);
        when(receipt.getSupplierCode()).thenReturn("SUP-50");
        when(receipt.getSupplierName()).thenReturn("Supplier 50");
        when(receipt.getCurrency()).thenReturn("NPR");

        GoodsReceiptLine line = mock(GoodsReceiptLine.class);
        when(line.getId()).thenReturn(GRN_LINE_ID);
        when(line.getItemCode()).thenReturn("ITEM-01");
        when(line.getReceivedQuantity()).thenReturn(new BigDecimal("10.00"));
        when(receipt.getLines()).thenReturn(List.of(line));

        when(receiptRepo.findByIdForUpdate(TENANT_ID, COMPANY_ID, GRN_ID)).thenReturn(Optional.of(receipt));

        CreateInvoiceRequest request = new CreateInvoiceRequest(
                GRN_ID,
                PurchaseInvoiceType.GOODS_RECEIPT,
                50L,
                1L,
                1L,
                "NPR",
                "INV-QTY-FAIL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                false,
                null,
                List.of(new CreateInvoiceRequest.Line(GRN_LINE_ID, null, null, null, null, new BigDecimal("15.00"), new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, "desc", null)),
                null
        );

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.create(TENANT_ID, COMPANY_ID, request))
                .withMessageContaining("3-way match failure: Billed quantity (15.00) exceeds received quantity (10.00)");
    }

    @Test
    @DisplayName("3-way match fails when billed price exceeds ordered price beyond 5% tolerance")
    void billedPriceExceedingFivePercentTolerance_failsThreeWayMatch() {
        GoodsReceipt receipt = mock(GoodsReceipt.class);
        when(receipt.getId()).thenReturn(GRN_ID);
        when(receipt.getStatus()).thenReturn(GoodsReceiptStatus.POSTED);
        when(receipt.getSupplierId()).thenReturn(50L);
        when(receipt.getSupplierCode()).thenReturn("SUP-50");
        when(receipt.getSupplierName()).thenReturn("Supplier 50");
        when(receipt.getCurrency()).thenReturn("NPR");

        GoodsReceiptLine line = mock(GoodsReceiptLine.class);
        when(line.getId()).thenReturn(GRN_LINE_ID);
        when(line.getItemCode()).thenReturn("ITEM-01");
        when(line.getReceivedQuantity()).thenReturn(new BigDecimal("10.00"));
        when(line.getUnitPrice()).thenReturn(new BigDecimal("100.00"));
        when(receipt.getLines()).thenReturn(List.of(line));

        when(receiptRepo.findByIdForUpdate(TENANT_ID, COMPANY_ID, GRN_ID)).thenReturn(Optional.of(receipt));

        // 100.00 * 1.05 = 105.00 max allowed. Billed at 110.00 (+10% variance)
        CreateInvoiceRequest request = new CreateInvoiceRequest(
                GRN_ID,
                PurchaseInvoiceType.GOODS_RECEIPT,
                50L,
                1L,
                1L,
                "NPR",
                "INV-PRICE-FAIL",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                false,
                null,
                List.of(new CreateInvoiceRequest.Line(GRN_LINE_ID, null, null, null, null, new BigDecimal("10.00"), new BigDecimal("110.00"), BigDecimal.ZERO, BigDecimal.ZERO, "desc", null)),
                null
        );

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> service.create(TENANT_ID, COMPANY_ID, request))
                .withMessageContaining("3-way match failure: Billed unit price (110.00) exceeds ordered price (100.00) by more than 5% tolerance");
    }
}
