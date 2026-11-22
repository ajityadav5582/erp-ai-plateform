package com.erp.business.procurement.supplierpayment;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.dto.ApplyAdvanceRequest;
import com.erp.business.procurement.supplierpayment.dto.CreatePaymentRequest;
import com.erp.business.procurement.supplierpayment.dto.PaymentAllocationRequest;
import com.erp.business.procurement.supplierpayment.dto.SupplierPaymentResponse;
import com.erp.business.procurement.supplierpayment.infrastructure.persistence.SupplierPaymentRepository;
import com.erp.platform.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierPaymentMultiInvoiceTest {

    @Mock
    private SupplierPaymentRepository paymentRepository;

    @Mock
    private PurchaseInvoiceRepository invoiceRepository;

    @Mock
    private com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnRepository purchaseReturnRepository;

    @Mock
    private DocumentSequenceRepository sequenceRepository;

    @Mock
    private SupplierService supplierService;

    private SupplierPaymentServiceImpl paymentService;

    private final Long tenantId = 1L;
    private final Long companyId = 100L;
    private final Long supplierId = 50L;
    private final String actor = "treasury_clerk_1";

    @BeforeEach
    void setUp() {
        paymentService = new SupplierPaymentServiceImpl(
                paymentRepository,
                invoiceRepository,
                purchaseReturnRepository,
                sequenceRepository,
                supplierService
        );
    }

    private void mockDocumentSequence() {
        DocumentSequence sequence = DocumentSequence.startAt(tenantId, companyId, "SUPPLIER_PAYMENT", "2081/82", 0);
        when(sequenceRepository.findForUpdate(eq(tenantId), eq(companyId), anyString(), anyString()))
                .thenReturn(Optional.of(sequence));
    }

    private PurchaseInvoice createMockInvoice(Long id, String invoiceNumber, BigDecimal total, BigDecimal paid) {
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .supplierId(supplierId)
                .supplierCode("SUP-50")
                .supplierName("ABC Suppliers Ltd")
                .currency("NPR")
                .totalAmount(total)
                .netPayableAmount(total)
                .paidAmount(paid)
                .balanceAmount(total.subtract(paid))
                .status(paid.signum() == 0 ? PurchaseInvoiceStatus.APPROVED : PurchaseInvoiceStatus.PARTIALLY_PAID)
                .branchId(10L)
                .fiscalYearId(2026L)
                .build();
        invoice.assignScope(tenantId, companyId);
        invoice.setId(id);
        return invoice;
    }

    @Test
    void payMultiInvoice_success_allocatesAcrossMultipleInvoicesAndCreatesAdvanceCredit() {
        mockDocumentSequence();

        PurchaseInvoice inv1 = createMockInvoice(10L, "INV-001", new BigDecimal("10000.0000"), BigDecimal.ZERO);
        PurchaseInvoice inv2 = createMockInvoice(20L, "INV-002", new BigDecimal("20000.0000"), BigDecimal.ZERO);

        when(invoiceRepository.findAllByIdForUpdate(eq(tenantId), eq(companyId), eq(List.of(10L, 20L))))
                .thenReturn(List.of(inv1, inv2));

        when(paymentRepository.saveAndFlush(any(SupplierPayment.class))).thenAnswer(invocation -> {
            SupplierPayment p = invocation.getArgument(0);
            p.setId(999L);
            return p;
        });

        // Pay 35,000 total: 10,000 for INV-001, 15,000 for INV-002, leaving 10,000 unallocated advance
        CreatePaymentRequest request = new CreatePaymentRequest(
                null,
                supplierId,
                new BigDecimal("35000.0000"),
                PaymentMethod.BANK_TRANSFER,
                LocalDate.now(),
                "UTR-998822",
                "Nabil Bank - 001",
                "Bulk settlement with advance",
                List.of(
                        new PaymentAllocationRequest(10L, new BigDecimal("10000.0000"), BigDecimal.ZERO, "Full settlement"),
                        new PaymentAllocationRequest(20L, new BigDecimal("15000.0000"), BigDecimal.ZERO, "Partial settlement")
                )
        );

        SupplierPaymentResponse response = paymentService.pay(tenantId, companyId, request, actor);

        assertNotNull(response);
        assertEquals(new BigDecimal("35000.0000"), response.amount());
        assertEquals(new BigDecimal("10000.0000"), response.unallocatedAmount());
        assertEquals(2, response.allocations().size());

        // Verify invoice balances were updated
        assertEquals(new BigDecimal("0.0000"), inv1.getBalanceAmount());
        assertEquals(PurchaseInvoiceStatus.PAID, inv1.getStatus());

        assertEquals(new BigDecimal("5000.0000"), inv2.getBalanceAmount());
        assertEquals(PurchaseInvoiceStatus.PARTIALLY_PAID, inv2.getStatus());

        // Verify payment was saved
        ArgumentCaptor<SupplierPayment> captor = ArgumentCaptor.forClass(SupplierPayment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        SupplierPayment saved = captor.getValue();
        assertEquals(2, saved.getAllocations().size());
        assertEquals(new BigDecimal("10000.0000"), saved.getUnallocatedAmount());
    }

    @Test
    void payMultiInvoice_canonicalOrdering_locksInvoicesInAscendingOrder() {
        mockDocumentSequence();

        PurchaseInvoice inv10 = createMockInvoice(10L, "INV-010", new BigDecimal("5000.0000"), BigDecimal.ZERO);
        PurchaseInvoice inv25 = createMockInvoice(25L, "INV-025", new BigDecimal("5000.0000"), BigDecimal.ZERO);

        // Even though allocations list passes 25 first then 10, repository MUST be queried with [10, 25]
        when(invoiceRepository.findAllByIdForUpdate(eq(tenantId), eq(companyId), eq(List.of(10L, 25L))))
                .thenReturn(List.of(inv10, inv25));

        when(paymentRepository.saveAndFlush(any(SupplierPayment.class))).thenAnswer(i -> i.getArgument(0));

        CreatePaymentRequest request = new CreatePaymentRequest(
                null,
                supplierId,
                new BigDecimal("10000.0000"),
                PaymentMethod.CHEQUE,
                LocalDate.now(),
                "CHQ-1234",
                "Branch Cash",
                "Test ordering",
                List.of(
                        new PaymentAllocationRequest(25L, new BigDecimal("5000.0000"), BigDecimal.ZERO, null),
                        new PaymentAllocationRequest(10L, new BigDecimal("5000.0000"), BigDecimal.ZERO, null)
                )
        );

        paymentService.pay(tenantId, companyId, request, actor);

        // Verify the lock was acquired with ascending [10L, 25L]
        verify(invoiceRepository).findAllByIdForUpdate(eq(tenantId), eq(companyId), eq(List.of(10L, 25L)));
    }

    @Test
    void payMultiInvoice_overAllocation_rejected() {
        PurchaseInvoice inv1 = createMockInvoice(10L, "INV-001", new BigDecimal("5000.0000"), BigDecimal.ZERO);

        when(invoiceRepository.findAllByIdForUpdate(eq(tenantId), eq(companyId), eq(List.of(10L))))
                .thenReturn(List.of(inv1));

        // Attempting to allocate 6,000 on a 5,000 invoice
        CreatePaymentRequest request = new CreatePaymentRequest(
                null,
                supplierId,
                new BigDecimal("6000.0000"),
                PaymentMethod.BANK_TRANSFER,
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new PaymentAllocationRequest(10L, new BigDecimal("6000.0000"), BigDecimal.ZERO, null))
        );

        assertThrows(ValidationException.class, () ->
                paymentService.pay(tenantId, companyId, request, actor));
    }

    @Test
    void paySingleInvoice_backwardCompatible() {
        mockDocumentSequence();

        PurchaseInvoice inv = createMockInvoice(10L, "INV-001", new BigDecimal("5000.0000"), BigDecimal.ZERO);
        when(invoiceRepository.findByIdForUpdate(tenantId, companyId, 10L)).thenReturn(Optional.of(inv));
        when(paymentRepository.saveAndFlush(any(SupplierPayment.class))).thenAnswer(i -> i.getArgument(0));

        CreatePaymentRequest legacyRequest = new CreatePaymentRequest(
                10L,
                new BigDecimal("5000.0000"),
                PaymentMethod.CASH,
                LocalDate.now(),
                null,
                null,
                "Legacy test"
        );

        SupplierPaymentResponse response = paymentService.pay(tenantId, companyId, legacyRequest, actor);

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.0000"), response.amount());
        assertEquals(new BigDecimal("0.0000"), inv.getBalanceAmount());
        assertEquals(PurchaseInvoiceStatus.PAID, inv.getStatus());
        assertEquals(1, response.allocations().size());
        assertEquals(10L, response.allocations().get(0).purchaseInvoiceId());
    }

    @Test
    void payAdvance_success_recordsPureAdvancePayment() {
        mockDocumentSequence();

        SupplierService.SupplierResponse mockSupplier = org.mockito.Mockito.mock(SupplierService.SupplierResponse.class);
        when(mockSupplier.getId()).thenReturn(supplierId);
        when(mockSupplier.getCode()).thenReturn("SUP-50");
        when(mockSupplier.getName()).thenReturn("ABC Suppliers Ltd");

        when(supplierService.findById(supplierId)).thenReturn(Optional.of(mockSupplier));
        when(paymentRepository.saveAndFlush(any(SupplierPayment.class))).thenAnswer(i -> {
            SupplierPayment p = i.getArgument(0);
            p.setId(888L);
            return p;
        });

        CreatePaymentRequest advanceRequest = new CreatePaymentRequest(
                null,
                supplierId,
                new BigDecimal("50000.0000"),
                PaymentMethod.BANK_TRANSFER,
                LocalDate.now(),
                "UTR-ADVANCE-999",
                "Main Operating Account",
                "Advance for upcoming material batches",
                null
        );

        SupplierPaymentResponse response = paymentService.pay(tenantId, companyId, advanceRequest, actor);

        assertNotNull(response);
        assertEquals(new BigDecimal("50000.0000"), response.amount());
        assertEquals(new BigDecimal("50000.0000"), response.unallocatedAmount());
        assertEquals(0, response.allocations().size());
        assertEquals("ABC Suppliers Ltd", response.supplierName());

        ArgumentCaptor<SupplierPayment> captor = ArgumentCaptor.forClass(SupplierPayment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        SupplierPayment saved = captor.getValue();
        assertEquals(new BigDecimal("50000.0000"), saved.getUnallocatedAmount());
        assertEquals(0, saved.getAllocations().size());
    }

    @Test
    void applyAdvance_success_offsetsInvoiceBalance() {
        SupplierPayment advancePayment = SupplierPayment.postMultiInvoice(
                "PAY-000050",
                LocalDate.now(),
                new BigDecimal("30000.0000"),
                new BigDecimal("30000.0000"),
                PaymentMethod.BANK_TRANSFER,
                "UTR-111",
                "Nabil Bank",
                supplierId,
                "SUP-50",
                "ABC Suppliers Ltd",
                "NPR",
                null,
                null,
                "Upfront advance",
                actor
        );
        advancePayment.assignScope(tenantId, companyId);
        advancePayment.setId(100L);

        PurchaseInvoice invoice = createMockInvoice(20L, "INV-2026-001", new BigDecimal("15000.0000"), BigDecimal.ZERO);

        when(paymentRepository.findByIdForUpdate(tenantId, companyId, 100L)).thenReturn(Optional.of(advancePayment));
        when(invoiceRepository.findByIdForUpdate(tenantId, companyId, 20L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.saveAndFlush(any(SupplierPayment.class))).thenAnswer(i -> i.getArgument(0));

        ApplyAdvanceRequest applyRequest = new ApplyAdvanceRequest(
                100L,
                20L,
                new BigDecimal("10000.0000"),
                "Apply 10k advance credit"
        );

        SupplierPaymentResponse response = paymentService.applyAdvance(tenantId, companyId, applyRequest, actor);

        assertNotNull(response);
        // Advance remaining balance reduced from 30k to 20k
        assertEquals(new BigDecimal("20000.0000"), advancePayment.getUnallocatedAmount());
        // Invoice balance reduced from 15k to 5k
        assertEquals(new BigDecimal("5000.0000"), invoice.getBalanceAmount());
        assertEquals(new BigDecimal("10000.0000"), invoice.getPaidAmount());
        assertEquals(PurchaseInvoiceStatus.PARTIALLY_PAID, invoice.getStatus());

        // An allocation line is added to the advance payment
        assertEquals(1, advancePayment.getAllocations().size());
        assertEquals(20L, advancePayment.getAllocations().get(0).getPurchaseInvoiceId());
        assertEquals(new BigDecimal("10000.0000"), advancePayment.getAllocations().get(0).getAllocatedAmount());

        verify(invoiceRepository).saveAndFlush(invoice);
        verify(paymentRepository).saveAndFlush(advancePayment);
    }

    @Test
    void applyAdvance_exceedsUnallocatedAmount_throwsValidationException() {
        SupplierPayment advancePayment = SupplierPayment.postMultiInvoice(
                "PAY-000051",
                LocalDate.now(),
                new BigDecimal("5000.0000"),
                new BigDecimal("5000.0000"), // only 5k available
                PaymentMethod.CASH,
                null,
                null,
                supplierId,
                "SUP-50",
                "ABC Suppliers Ltd",
                "NPR",
                null,
                null,
                "Small advance",
                actor
        );
        advancePayment.assignScope(tenantId, companyId);
        advancePayment.setId(101L);

        when(paymentRepository.findByIdForUpdate(tenantId, companyId, 101L)).thenReturn(Optional.of(advancePayment));

        ApplyAdvanceRequest applyRequest = new ApplyAdvanceRequest(
                101L,
                20L,
                new BigDecimal("8000.0000"), // requesting 8k > 5k
                "Over apply"
        );

        assertThrows(ValidationException.class, () ->
                paymentService.applyAdvance(tenantId, companyId, applyRequest, actor));
    }

    @Test
    void applyAdvance_supplierMismatch_throwsValidationException() {
        SupplierPayment advancePayment = SupplierPayment.postMultiInvoice(
                "PAY-000052",
                LocalDate.now(),
                new BigDecimal("20000.0000"),
                new BigDecimal("20000.0000"),
                PaymentMethod.BANK_TRANSFER,
                null,
                null,
                supplierId, // supplier 50
                "SUP-50",
                "ABC Suppliers Ltd",
                "NPR",
                null,
                null,
                "Advance for SUP-50",
                actor
        );
        advancePayment.assignScope(tenantId, companyId);
        advancePayment.setId(102L);

        PurchaseInvoice invoiceOtherSupplier = PurchaseInvoice.builder()
                .invoiceNumber("INV-999")
                .supplierId(999L) // different supplier 999
                .supplierCode("SUP-999")
                .supplierName("Different Supplier")
                .currency("NPR")
                .totalAmount(new BigDecimal("10000.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceAmount(new BigDecimal("10000.0000"))
                .status(PurchaseInvoiceStatus.APPROVED)
                .build();
        invoiceOtherSupplier.assignScope(tenantId, companyId);
        invoiceOtherSupplier.setId(500L);

        when(paymentRepository.findByIdForUpdate(tenantId, companyId, 102L)).thenReturn(Optional.of(advancePayment));
        when(invoiceRepository.findByIdForUpdate(tenantId, companyId, 500L)).thenReturn(Optional.of(invoiceOtherSupplier));

        ApplyAdvanceRequest applyRequest = new ApplyAdvanceRequest(
                102L,
                500L,
                new BigDecimal("5000.0000"),
                "Mismatched supplier"
        );

        assertThrows(ValidationException.class, () ->
                paymentService.applyAdvance(tenantId, companyId, applyRequest, actor));
    }

    @Test
    void getSupplierBalanceSummaryCalculatesPayablesAndAdvances() {
        PurchaseInvoice inv1 = PurchaseInvoice.builder()
                .balanceAmount(new BigDecimal("15000.0000"))
                .currency("NPR")
                .status(PurchaseInvoiceStatus.APPROVED)
                .build();
        PurchaseInvoice inv2 = PurchaseInvoice.builder()
                .balanceAmount(new BigDecimal("25000.0000"))
                .currency("NPR")
                .status(PurchaseInvoiceStatus.PARTIALLY_PAID)
                .build();

        when(invoiceRepository.findOutstandingBySupplier(eq(tenantId), eq(companyId), eq(supplierId), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(inv1, inv2)));

        SupplierPayment adv = SupplierPayment.builder()
                .unallocatedAmount(new BigDecimal("8000.0000"))
                .currency("NPR")
                .build();

        when(paymentRepository.findAvailableAdvances(tenantId, companyId, supplierId))
                .thenReturn(List.of(adv));

        var summary = paymentService.getSupplierBalanceSummary(tenantId, companyId, supplierId);

        assertEquals(supplierId, summary.supplierId());
        assertEquals(new BigDecimal("40000.0000"), summary.totalOutstandingPayable());
        assertEquals(new BigDecimal("8000.0000"), summary.totalAvailableAdvance());
        assertEquals(2, summary.unpaidInvoiceCount());
        assertEquals(1, summary.availableAdvanceCount());
        assertEquals("NPR", summary.currency());
    }
}
