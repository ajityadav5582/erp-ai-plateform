package com.erp.business.procurement.supplierpayment;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnRepository;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.dto.SupplierStatementResponse;
import com.erp.business.procurement.supplierpayment.infrastructure.persistence.SupplierPaymentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierStatementServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final Long SUPPLIER_ID = 101L;

    @Mock
    private SupplierPaymentRepository paymentRepository;
    @Mock
    private PurchaseInvoiceRepository invoiceRepository;
    @Mock
    private PurchaseReturnRepository purchaseReturnRepository;
    @Mock
    private DocumentSequenceRepository sequenceRepository;
    @Mock
    private SupplierService supplierService;

    private SupplierPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupplierPaymentServiceImpl(
                paymentRepository,
                invoiceRepository,
                purchaseReturnRepository,
                sequenceRepository,
                supplierService
        );
    }

    @Test
    @DisplayName("generates accurate running statement with invoices, payments, and debit notes")
    void generatesAccurateSupplierStatement() {
        SupplierService.SupplierResponse mockSupplier = mock(SupplierService.SupplierResponse.class);
        when(mockSupplier.getCode()).thenReturn("SUP-101");
        when(mockSupplier.getName()).thenReturn("Himalayan Traders");
        when(supplierService.findById(SUPPLIER_ID)).thenReturn(Optional.of(mockSupplier));

        LocalDate day1 = LocalDate.of(2026, 1, 10);
        LocalDate day2 = LocalDate.of(2026, 1, 15);
        LocalDate day3 = LocalDate.of(2026, 1, 20);

        // Invoice 1: 50,000 billed on Jan 10
        PurchaseInvoice inv = mock(PurchaseInvoice.class);
        when(inv.getId()).thenReturn(1L);
        when(inv.getStatus()).thenReturn(PurchaseInvoiceStatus.APPROVED);
        when(inv.getInvoiceDate()).thenReturn(day1);
        when(inv.getInvoiceNumber()).thenReturn("INV-001");
        when(inv.getNetPayableAmount()).thenReturn(new BigDecimal("50000.00"));

        // Payment 1: 30,000 paid on Jan 15
        SupplierPayment pay = mock(SupplierPayment.class);
        when(pay.getId()).thenReturn(10L);
        when(pay.getStatus()).thenReturn(com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus.POSTED);
        when(pay.getPaymentDate()).thenReturn(day2);
        when(pay.getPaymentNumber()).thenReturn("PAY-001");
        when(pay.getReferenceNo()).thenReturn("BANK-TXN-123");
        when(pay.getAmount()).thenReturn(new BigDecimal("30000.00"));
        when(pay.getPaymentMethod()).thenReturn(PaymentMethod.BANK_TRANSFER);
        when(pay.getPaidFrom()).thenReturn("Nabil Bank");

        // Purchase Return / Debit Note: 5,000 returned on Jan 20
        PurchaseReturn ret = mock(PurchaseReturn.class);
        when(ret.getId()).thenReturn(20L);
        when(ret.getStatus()).thenReturn(PurchaseReturnStatus.POSTED);
        when(ret.getReturnDate()).thenReturn(day3);
        when(ret.getDebitNoteNumber()).thenReturn("DN-001");
        when(ret.getInvoiceNumber()).thenReturn("INV-001");
        when(ret.getTotalAmount()).thenReturn(new BigDecimal("5000.00"));

        when(invoiceRepository.findByTenantIdAndCompanyIdAndSupplierId(TENANT_ID, COMPANY_ID, SUPPLIER_ID))
                .thenReturn(List.of(inv));
        when(paymentRepository.findByTenantIdAndCompanyIdAndSupplierId(TENANT_ID, COMPANY_ID, SUPPLIER_ID))
                .thenReturn(List.of(pay));
        when(purchaseReturnRepository.findByTenantIdAndCompanyIdAndSupplierId(TENANT_ID, COMPANY_ID, SUPPLIER_ID))
                .thenReturn(List.of(ret));

        SupplierStatementResponse statement = service.getSupplierStatement(
                TENANT_ID, COMPANY_ID, SUPPLIER_ID, null, null);

        assertThat(statement.supplierCode()).isEqualTo("SUP-101");
        assertThat(statement.supplierName()).isEqualTo("Himalayan Traders");
        assertThat(statement.openingBalance()).isEqualByComparingTo("0.00");
        assertThat(statement.totalInvoiced()).isEqualByComparingTo("50000.00");
        assertThat(statement.totalPaid()).isEqualByComparingTo("30000.00");
        assertThat(statement.totalReturns()).isEqualByComparingTo("5000.00");
        // Closing balance = 50,000 - 30,000 - 5,000 = 15,000
        assertThat(statement.closingBalance()).isEqualByComparingTo("15000.00");

        assertThat(statement.lines()).hasSize(3);
        // Line 1: Invoice +50,000 -> Running: 50,000
        assertThat(statement.lines().get(0).creditAmount()).isEqualByComparingTo("50000.00");
        assertThat(statement.lines().get(0).runningBalance()).isEqualByComparingTo("50000.00");
        // Line 2: Payment -30,000 -> Running: 20,000
        assertThat(statement.lines().get(1).debitAmount()).isEqualByComparingTo("30000.00");
        assertThat(statement.lines().get(1).runningBalance()).isEqualByComparingTo("20000.00");
        // Line 3: Return -5,000 -> Running: 15,000
        assertThat(statement.lines().get(2).debitAmount()).isEqualByComparingTo("5000.00");
        assertThat(statement.lines().get(2).runningBalance()).isEqualByComparingTo("15000.00");
    }
}

