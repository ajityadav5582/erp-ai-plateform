package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoodsReceiptFixtureTest {

    @Test
    void createsPostedReceiptWithOneLine() {
        PurchaseOrder order = Mockito.mock(PurchaseOrder.class);
        Mockito.when(order.getId()).thenReturn(1L);
        Mockito.when(order.getPoNumber()).thenReturn("PO-1");
        Mockito.when(order.getFiscalYearId()).thenReturn(1L);
        Mockito.when(order.getBranchId()).thenReturn(1L);
        Mockito.when(order.getSupplierId()).thenReturn(1L);
        Mockito.when(order.getSupplierCode()).thenReturn("SUP");
        Mockito.when(order.getSupplierName()).thenReturn("Supplier");
        Mockito.when(order.getCurrency()).thenReturn("NPR");

        GoodsReceipt receipt = GoodsReceipt.post(
                "GRN-1", LocalDate.now(), order, 1L, "WH", "Warehouse", null, null, "actor");
        receipt.assignScope(1L, 1L);

        GoodsReceiptLine realLine = GoodsReceiptLine.builder()
                .lineNumber(1)
                .purchaseOrderLineId(1L)
                .itemId(1L)
                .itemCode("ITEM")
                .itemName("Item")
                .uomCode("PCS")
                .receivedQuantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("100"))
                .lineAmount(new BigDecimal("200"))
                .build();
        GoodsReceiptLine line = Mockito.spy(realLine);
        Mockito.doReturn(1L).when(line).getId();
        receipt.addLine(line);

        assertEquals(GoodsReceiptStatus.POSTED, receipt.getStatus());
        assertNotNull(receipt.getGrnNumber());
    }
}
