package com.erp.business.procurement.purchasereturn;

import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnLine;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnResponse;
import com.erp.business.procurement.purchasereturn.dto.ReturnDecisionRequest;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnLineRepository;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnRepository;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus;
import com.erp.business.procurement.supplierpayment.infrastructure.persistence.SupplierPaymentRepository;
import com.erp.platform.business.context.BusinessContextAccessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseReturnAdvanceCreditTest {

    @Mock
    private PurchaseReturnRepository returnRepository;
    @Mock
    private PurchaseReturnLineRepository returnLineRepository;
    @Mock
    private PurchaseInvoiceRepository invoiceRepository;
    @Mock
    private GoodsReceiptRepository receiptRepository;
    @Mock
    private DocumentSequenceRepository sequenceRepository;
    @Mock
    private StockMovementService stockMovementService;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private PurchaseReturnMapper returnMapper;
    @Mock
    private BusinessContextAccessor context;
    @Mock
    private SupplierPaymentRepository supplierPaymentRepository;

    @InjectMocks
    private PurchaseReturnServiceImpl returnService;

    private final Long tenantId = 1L;
    private final Long companyId = 10L;

    @Test
    @DisplayName("postReturn on fully paid invoice converts Debit Note amount into supplier advance credit")
    void postReturn_onFullyPaidInvoice_createsSupplierAdvanceCredit() {
        Long returnId = 100L;
        Long invoiceId = 200L;

        PurchaseReturn purchaseReturn = mock(PurchaseReturn.class);
        when(purchaseReturn.getId()).thenReturn(returnId);
        when(purchaseReturn.getPurchaseInvoiceId()).thenReturn(invoiceId);
        when(purchaseReturn.getStatus()).thenReturn(PurchaseReturnStatus.APPROVED);
        when(purchaseReturn.getTotalAmount()).thenReturn(new BigDecimal("5000.00"));
        when(purchaseReturn.getDebitNoteNumber()).thenReturn("DN-0001");
        when(purchaseReturn.getSupplierId()).thenReturn(50L);
        when(purchaseReturn.getSupplierCode()).thenReturn("SUP-50");
        when(purchaseReturn.getSupplierName()).thenReturn("Alpha Traders");
        when(purchaseReturn.getCurrency()).thenReturn("NPR");

        when(returnRepository.findByIdForUpdate(tenantId, companyId, returnId))
                .thenReturn(Optional.of(purchaseReturn));
        when(returnRepository.save(any(PurchaseReturn.class))).thenReturn(purchaseReturn);
        when(returnRepository.findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, returnId))
                .thenReturn(Optional.of(purchaseReturn));

        PurchaseInvoice invoice = mock(PurchaseInvoice.class);
        // Invoice was already paid, so current balance is ZERO
        when(invoice.getBalanceAmount()).thenReturn(BigDecimal.ZERO);
        when(invoice.getInvoiceNumber()).thenReturn("INV-PAID-01");
        when(invoiceRepository.findByIdForUpdate(tenantId, companyId, invoiceId))
                .thenReturn(Optional.of(invoice));

        StockMovementBatchResponse batchResp = mock(StockMovementBatchResponse.class);
        when(stockMovementService.postIssue(any(StockReceiptRequest.class))).thenReturn(batchResp);

        returnService.postReturn(tenantId, companyId, returnId);

        // Verify debit note applied to invoice
        verify(invoice).recordDebitNote(new BigDecimal("5000.00"));
        verify(invoiceRepository).save(invoice);

        // Verify supplier advance credit generated for the full 5000.00
        ArgumentCaptor<SupplierPayment> captor = ArgumentCaptor.forClass(SupplierPayment.class);
        verify(supplierPaymentRepository).save(captor.capture());

        SupplierPayment credit = captor.getValue();
        assertThat(credit.getPaymentNumber()).isEqualTo("ADV-DN-DN-0001");
        assertThat(credit.getSupplierId()).isEqualTo(50L);
        assertThat(credit.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(credit.getUnallocatedAmount()).isEqualByComparingTo("5000.00");
        assertThat(credit.getStatus()).isEqualTo(SupplierPaymentStatus.POSTED);
        assertThat(credit.getReferenceNo()).isEqualTo("DN-DN-0001");
    }

    @Test
    @DisplayName("cancelReturn reverses invoice debit note and cancels generated advance credit")
    void cancelReturn_cancelsGeneratedAdvanceCredit() {
        Long returnId = 100L;
        Long invoiceId = 200L;

        PurchaseReturn purchaseReturn = mock(PurchaseReturn.class);
        when(purchaseReturn.getPurchaseInvoiceId()).thenReturn(invoiceId);
        when(purchaseReturn.getStatus()).thenReturn(PurchaseReturnStatus.POSTED);
        when(purchaseReturn.getTotalAmount()).thenReturn(new BigDecimal("5000.00"));
        when(purchaseReturn.getDebitNoteNumber()).thenReturn("DN-0001");

        when(returnRepository.findByIdForUpdate(tenantId, companyId, returnId))
                .thenReturn(Optional.of(purchaseReturn));
        when(returnRepository.save(any(PurchaseReturn.class))).thenReturn(purchaseReturn);

        PurchaseInvoice invoice = mock(PurchaseInvoice.class);
        when(invoiceRepository.findByIdForUpdate(tenantId, companyId, invoiceId))
                .thenReturn(Optional.of(invoice));

        SupplierPayment existingCredit = mock(SupplierPayment.class);
        when(supplierPaymentRepository.findByTenantIdAndCompanyIdAndReferenceNo(
                tenantId, companyId, "DN-DN-0001")).thenReturn(Optional.of(existingCredit));

        returnService.cancelReturn(tenantId, companyId, returnId, new ReturnDecisionRequest("Wrong return"));

        verify(invoice).reverseDebitNote(new BigDecimal("5000.00"));
        verify(existingCredit).cancel(any(), eq("Purchase return DN-0001 was cancelled"));
        verify(supplierPaymentRepository).save(existingCredit);
    }
}

