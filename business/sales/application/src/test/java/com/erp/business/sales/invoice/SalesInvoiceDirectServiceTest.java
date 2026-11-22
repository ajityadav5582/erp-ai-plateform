package com.erp.business.sales.invoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.sales.customer.infrastructure.persistence.CustomerRepository;
import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import com.erp.business.sales.invoice.domain.VatType;
import com.erp.business.sales.invoice.dto.CreateDirectSalesInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceLineRequest;
import com.erp.business.sales.invoice.dto.SalesInvoiceResponse;
import com.erp.business.sales.invoice.infrastructure.persistence.SalesInvoiceRepository;
import com.erp.business.sales.invoice.mapper.SalesInvoiceMapper;
import com.erp.business.sales.invoice.mapper.TaxInvoiceMapper;
import com.erp.business.sales.salesorder.SalesOrderService;
import com.erp.business.sales.shared.numbering.DocumentNumberService;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import com.erpai.integration.ird.outbox.service.IrdOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesInvoiceDirectServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COMPANY_ID = 10L;

    @Mock
    private SalesInvoiceRepository invoiceRepository;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private ItemService itemService;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SalesInvoiceMapper invoiceMapper;
    @Mock
    private TaxInvoiceMapper taxInvoiceMapper;
    @Mock
    private DocumentNumberService documentNumberService;
    @Mock
    private BusinessContextAccessor context;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private TaxRateService taxRateService;
    @Mock
    private IrdOutboxService irdOutboxService;
    @Mock
    private StockMovementService stockMovementService;

    private SalesInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SalesInvoiceServiceImpl(
                invoiceRepository,
                salesOrderService,
                itemService,
                customerRepository,
                invoiceMapper,
                taxInvoiceMapper,
                documentNumberService,
                context,
                companyRepository,
                taxRateService,
                irdOutboxService,
                stockMovementService
        );
        lenient().when(context.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(context.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(context.getUserId()).thenReturn("test-user");
        lenient().when(documentNumberService.nextNumber(any(), any(), any(), any(), any())).thenReturn("INV-1001");
    }

    @Test
    @DisplayName("createDirectInvoice creates, issues, decrements warehouse stock and enqueues to IRD")
    void createDirectInvoice_success_withStockMovement() {
        ItemService.ItemResponse mockItem = mock(ItemService.ItemResponse.class);
        when(mockItem.getId()).thenReturn(101L);
        when(mockItem.getSku()).thenReturn("TAP-01");
        when(mockItem.getName()).thenReturn("Brass Tap");
        when(mockItem.getIsStockable()).thenReturn(true);
        when(mockItem.getIsVatInclusive()).thenReturn(false);
        when(itemService.findById(101L)).thenReturn(Optional.of(mockItem));

        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(inv -> {
            SalesInvoice invoice = inv.getArgument(0);
            if (invoice.getId() == null) {
                ReflectionTestUtils.setField(invoice, "id", 501L);
            }
            return invoice;
        });

        SalesInvoiceResponse mockResponse = mock(SalesInvoiceResponse.class);
        when(invoiceMapper.toResponse(any(SalesInvoice.class))).thenReturn(mockResponse);

        InvoiceLineRequest line = new InvoiceLineRequest(
                101L, "PCS", new BigDecimal("5.000000"), new BigDecimal("450.0000"),
                BigDecimal.ZERO, VatType.TAXABLE, new BigDecimal("13.00"), "Brass Tap 1/2"
        );

        CreateDirectSalesInvoiceRequest request = new CreateDirectSalesInvoiceRequest(
                null, "Walk-in Retail Buyer", "123456789", 5L,
                LocalDate.now(), LocalDate.now(), true, "CASH", "CASH-001", "Counter Cash Sale",
                List.of(line)
        );

        SalesInvoiceResponse result = service.createDirectInvoice(TENANT_ID, COMPANY_ID, request);
        assertThat(result).isNotNull();

        // Verify stock movement posted with reference SALES_INVOICE
        ArgumentCaptor<StockReceiptRequest> stockCaptor = ArgumentCaptor.forClass(StockReceiptRequest.class);
        verify(stockMovementService).postIssue(stockCaptor.capture());
        StockReceiptRequest capturedStock = stockCaptor.getValue();
        assertThat(capturedStock.referenceType()).isEqualTo("SALES_INVOICE");
        assertThat(capturedStock.referenceId()).isEqualTo(501L);
        assertThat(capturedStock.lines()).hasSize(1);
        assertThat(capturedStock.lines().get(0).warehouseId()).isEqualTo(5L);
        assertThat(capturedStock.lines().get(0).itemId()).isEqualTo(101L);
        assertThat(capturedStock.lines().get(0).quantity()).isEqualByComparingTo("5");

        // Verify IRD Outbox enqueue
        verify(irdOutboxService).enqueueSalesBill(eq(TENANT_ID), eq(COMPANY_ID), eq(501L), any(), any());

        // Verify payment settled since isSettled = true
        ArgumentCaptor<SalesInvoice> invoiceCaptor = ArgumentCaptor.forClass(SalesInvoice.class);
        verify(invoiceRepository, atLeastOnce()).save(invoiceCaptor.capture());
        SalesInvoice lastSaved = invoiceCaptor.getValue();
        assertThat(lastSaved.getStatus()).isEqualTo(SalesInvoiceStatus.PAID);
        assertThat(lastSaved.getBalanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("createDirectInvoice with unit price below min selling price floor throws IllegalArgumentException")
    void createDirectInvoice_belowMinSellingPriceFloor_throwsException() {
        ItemService.ItemResponse mockItem = mock(ItemService.ItemResponse.class);
        when(mockItem.getId()).thenReturn(102L);
        when(mockItem.getName()).thenReturn("Premium Pipe");
        when(mockItem.getIsVatInclusive()).thenReturn(false);
        // Floor price is 400.00
        when(mockItem.getMinSellingPrice()).thenReturn(new BigDecimal("400.00"));
        when(itemService.findById(102L)).thenReturn(Optional.of(mockItem));

        // Invoice attempts to sell at 350.00 (below 400.00 floor)
        InvoiceLineRequest line = new InvoiceLineRequest(
                102L, "PCS", new BigDecimal("1.000000"), new BigDecimal("350.0000"),
                BigDecimal.ZERO, VatType.TAXABLE, new BigDecimal("13.00"), "Below Floor Pipe"
        );

        CreateDirectSalesInvoiceRequest request = new CreateDirectSalesInvoiceRequest(
                null, "Bargain Buyer", null, 5L,
                LocalDate.now(), LocalDate.now(), false, null, null, "Below Floor Sale",
                List.of(line)
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createDirectInvoice(TENANT_ID, COMPANY_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is below minimum selling price floor (400.00)");
    }
}
