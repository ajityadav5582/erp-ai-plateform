package com.erp.business.pos.invoice;

import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.pos.domain.PosInvoice;
import com.erp.business.pos.domain.PosInvoiceSequence;
import com.erp.business.pos.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.pos.invoice.infrastructure.persistence.PosInvoiceRepository;
import com.erp.business.pos.invoice.infrastructure.persistence.PosInvoiceSequenceRepository;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosInvoiceServiceImplVatTest {

    @Mock PosInvoiceRepository invoiceRepository;
    @Mock PosInvoiceSequenceRepository sequenceRepository;
    @Mock CompanyRepository companyRepository;
    @Mock TaxRateService taxRateService;
    @Mock ItemService itemService;

    PosInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PosInvoiceServiceImpl(invoiceRepository, sequenceRepository, companyRepository, taxRateService, itemService);
        PosInvoiceSequence seq = PosInvoiceSequence.builder().fiscalYear("2083/84").prefix("RAM").lastNumber(0L).build();
        when(invoiceRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(sequenceRepository.findForUpdate(any(), any(), any())).thenReturn(Optional.of(seq));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Company vatCompany(boolean effective) {
        Company c = org.mockito.Mockito.mock(Company.class);
        when(c.isVatEffectiveAsOf(any())).thenReturn(effective);
        return c;
    }

    private TaxRateService.TaxRate taxRate() {
        return new TaxRateService.TaxRate(1L, "VAT", "VAT", new BigDecimal("13.00"), "STANDARD", LocalDate.now(), null);
    }

    private ItemService.ItemResponse item(String taxability, boolean vatInclusive) {
        ItemService.ItemResponse it = org.mockito.Mockito.mock(ItemService.ItemResponse.class);
        when(it.getTaxabilityType()).thenReturn(taxability);
        when(it.getIsVatInclusive()).thenReturn(vatInclusive);
        return it;
    }

    @Test
    void vatExclusiveTaxable() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(taxRateService.findActive(1L, 1L, "VAT", LocalDate.now())).thenReturn(taxRate());
        ItemService.ItemResponse it = item("TAXABLE", false);
        when(itemService.findById(1L)).thenReturn(Optional.of(it));

        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("100"), null, true)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(0, inv.getGrossTotal().compareTo(new BigDecimal("200")));
        assertEquals(0, inv.getVatAmount().compareTo(new BigDecimal("26")));
        assertEquals(0, inv.getGrandTotal().compareTo(new BigDecimal("226")));
    }

    @Test
    void vatInclusiveTaxable() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(taxRateService.findActive(1L, 1L, "VAT", LocalDate.now())).thenReturn(taxRate());
        ItemService.ItemResponse it = item("TAXABLE", true);
        when(itemService.findById(1L)).thenReturn(Optional.of(it));

        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("113"), null, true)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(0, inv.getVatAmount().compareTo(new BigDecimal("26")));
    }

    @Test
    void discountBeforeVat() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(taxRateService.findActive(1L, 1L, "VAT", LocalDate.now())).thenReturn(taxRate());
        ItemService.ItemResponse it = item("TAXABLE", false);
        when(itemService.findById(1L)).thenReturn(Optional.of(it));

        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("20"), true)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(new BigDecimal("23.4000"), inv.getVatAmount());
    }

    @Test
    void negativeDiscountRejected() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("-5"), true)));
        assertThrows(IllegalArgumentException.class, () -> service.createInvoice(1L, 1L, req));
    }

    @Test
    void excessiveDiscountRejected() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("300"), true)));
        assertThrows(IllegalArgumentException.class, () -> service.createInvoice(1L, 1L, req));
    }

    @Test
    void exemptAndZeroRated() {
        Company company = vatCompany(true);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(taxRateService.findActive(1L, 1L, "VAT", LocalDate.now())).thenReturn(taxRate());
        ItemService.ItemResponse it1 = item("EXEMPT", false);
        ItemService.ItemResponse it2 = item("ZERO_RATED", false);
        when(itemService.findById(1L)).thenReturn(Optional.of(it1));
        when(itemService.findById(2L)).thenReturn(Optional.of(it2));

        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(
                        new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("1"), new BigDecimal("50"), null, false),
                        new CreatePosInvoiceRequest.InvoiceLine(2L, "PCS", new BigDecimal("1"), new BigDecimal("60"), null, false)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(0, inv.getExemptAmount().compareTo(new BigDecimal("50")));
        assertEquals(0, inv.getZeroRatedAmount().compareTo(new BigDecimal("60")));
        assertEquals(BigDecimal.ZERO, inv.getVatAmount());
    }

    @Test
    void nonVatCompany() {
        Company company = vatCompany(false);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        ItemService.ItemResponse it = item("TAXABLE", false);
        when(itemService.findById(1L)).thenReturn(Optional.of(it));

        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(UUID.randomUUID(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("2"), new BigDecimal("100"), null, true)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(BigDecimal.ZERO, inv.getVatAmount());
    }

    @Test
    void idempotency() {
        PosInvoice existing = PosInvoice.builder().idempotencyKey(UUID.randomUUID()).build();
        when(invoiceRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(existing));
        CreatePosInvoiceRequest req = new CreatePosInvoiceRequest(existing.getIdempotencyKey(), 1L, 1L, null, null, "CASH",
                null, null, List.of(new CreatePosInvoiceRequest.InvoiceLine(1L, "PCS", new BigDecimal("1"), new BigDecimal("10"), null, true)));
        PosInvoice inv = service.createInvoice(1L, 1L, req);
        assertEquals(existing, inv);
    }
}
