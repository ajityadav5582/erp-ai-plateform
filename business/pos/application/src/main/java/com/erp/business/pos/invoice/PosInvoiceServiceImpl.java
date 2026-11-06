package com.erp.business.pos.invoice;

import com.erp.business.pos.domain.PosInvoice;
import com.erp.business.pos.domain.PosInvoiceSequence;
import com.erp.business.pos.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.pos.invoice.infrastructure.persistence.PosInvoiceRepository;
import com.erp.business.pos.invoice.infrastructure.persistence.PosInvoiceSequenceRepository;
import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.infrastructure.persistence.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class PosInvoiceServiceImpl implements PosInvoiceService {

    private final PosInvoiceRepository invoiceRepository;
    private final PosInvoiceSequenceRepository sequenceRepository;
    private final CompanyRepository companyRepository;
    private final TaxRateService taxRateService;
    private final ItemService itemService;

    public PosInvoiceServiceImpl(PosInvoiceRepository invoiceRepository,
                                PosInvoiceSequenceRepository sequenceRepository,
                                CompanyRepository companyRepository,
                                TaxRateService taxRateService) {
        this(invoiceRepository, sequenceRepository, companyRepository, taxRateService, null);
    }

    public PosInvoiceServiceImpl(PosInvoiceRepository invoiceRepository,
                                PosInvoiceSequenceRepository sequenceRepository,
                                CompanyRepository companyRepository,
                                TaxRateService taxRateService,
                                ItemService itemService) {
        this.invoiceRepository = invoiceRepository;
        this.sequenceRepository = sequenceRepository;
        this.companyRepository = companyRepository;
        this.taxRateService = taxRateService;
        this.itemService = itemService;
    }

    @Override
    public PosInvoice createInvoice(Long tenantId, Long companyId, CreatePosInvoiceRequest request) {
        // Idempotency check
        return invoiceRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> generateInvoice(tenantId, companyId, request));
    }

    private PosInvoice generateInvoice(Long tenantId, Long companyId, CreatePosInvoiceRequest request) {
        LocalDate invoiceDate = LocalDate.now();
        String fiscalYear = resolveFiscalYear(invoiceDate);
        String prefix = "RAM";

        PosInvoiceSequence seq = sequenceRepository.findForUpdate(tenantId, companyId, fiscalYear)
                .orElseGet(() -> {
                    PosInvoiceSequence s = PosInvoiceSequence.builder()
                            .fiscalYear(fiscalYear)
                            .prefix(prefix)
                            .lastNumber(0L)
                            .build();
                    s.assignScope(tenantId, companyId);
                    return sequenceRepository.save(s);
                });

        Long number = seq.nextNumber();
        String invoiceNumber = String.format("%s-%s-%07d", prefix, fiscalYear, number);

        boolean vatApplicable = companyRepository.findById(companyId)
                .map(c -> c.isVatEffectiveAsOf(java.time.LocalDate.now()))
                .orElse(false);

        // Validate discounts before any tax-rate lookup.
        for (CreatePosInvoiceRequest.InvoiceLine l : request.items()) {
            BigDecimal lineGrossRaw = l.unitPrice().multiply(l.quantity());
            BigDecimal lineDiscount = l.discountAmount() == null ? BigDecimal.ZERO : l.discountAmount();
            if (lineDiscount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Discount amount cannot be negative");
            }
            if (lineDiscount.compareTo(lineGrossRaw) > 0) {
                throw new IllegalArgumentException("Discount amount cannot exceed line gross amount");
            }
        }

        com.erp.business.inventory.interfaces.tax.TaxRateService.TaxRate tr = null;
        BigDecimal vatRatePercent = BigDecimal.ZERO;
        if (vatApplicable) {
            tr = taxRateService.findActive(tenantId, companyId, "VAT", java.time.LocalDate.now());
            if (tr == null) {
                throw new IllegalStateException("No active VAT tax rate configured for company " + companyId);
            }
            vatRatePercent = tr.rate();
        }
        BigDecimal vatRateMultiplier = vatRatePercent.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal vatRateDivisorFactor = vatRatePercent.divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP);

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;
        BigDecimal zeroRated = BigDecimal.ZERO;
        for (CreatePosInvoiceRequest.InvoiceLine l : request.items()) {
            BigDecimal lineGrossRaw = l.unitPrice().multiply(l.quantity());
            BigDecimal lineDiscount = l.discountAmount() == null ? BigDecimal.ZERO : l.discountAmount();
            if (lineDiscount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Discount amount cannot be negative");
            }
            if (lineDiscount.compareTo(lineGrossRaw) > 0) {
                throw new IllegalArgumentException("Discount amount cannot exceed line gross amount");
            }
            discount = discount.add(lineDiscount);

            ItemService.ItemResponse item = null;
            if (itemService != null) {
                item = itemService.findById(l.itemId()).orElse(null);
            }
            String taxability = l.isTaxable() ? "TAXABLE" : "EXEMPT";
            if (item != null && item.getTaxabilityType() != null) {
                taxability = item.getTaxabilityType().toUpperCase();
            }

            boolean inclusive = item != null && Boolean.TRUE.equals(item.getIsVatInclusive());

            BigDecimal baseGross = lineGrossRaw;
            if (vatApplicable && "TAXABLE".equals(taxability) && inclusive && vatRatePercent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = BigDecimal.ONE.add(vatRateDivisorFactor);
                baseGross = lineGrossRaw.divide(divisor, 4, java.math.RoundingMode.HALF_UP);
            }

            gross = gross.add(baseGross);

            if (vatApplicable) {
                BigDecimal lineBase = baseGross.subtract(lineDiscount).max(BigDecimal.ZERO);
                if ("TAXABLE".equals(taxability)) {
                    taxable = taxable.add(lineBase);
                } else if ("EXEMPT".equals(taxability)) {
                    exempt = exempt.add(lineBase);
                } else if ("ZERO_RATED".equals(taxability)) {
                    zeroRated = zeroRated.add(lineBase);
                }
            }
        }

        BigDecimal vat = BigDecimal.ZERO;
        if (vatApplicable && taxable.compareTo(BigDecimal.ZERO) > 0) {
            vat = taxable.multiply(vatRateMultiplier);
        }
        BigDecimal grand = gross.subtract(discount).add(vat);

        String linesJson = request.items().toString();

        PosInvoice invoice = PosInvoice.builder()
                .idempotencyKey(request.idempotencyKey())
                .invoiceNumber(invoiceNumber)
                .registerId(request.registerId())
                .sessionId(request.sessionId())
                .customerId(request.customerId())
                .customerPan(request.customerPan())
                .paymentMethod(request.paymentMethod())
                .tenderedAmount(request.tenderedAmount())
                .changeAmount(request.changeAmount())
                .grossTotal(gross)
                .discountAmount(discount)
                .taxableAmount(taxable)
                .exemptAmount(exempt)
                .zeroRatedAmount(zeroRated)
                .vatAmount(vat)
                .grandTotal(grand)
                .bsDate(NepaliDateConverter.format(invoiceDate)) // BS date from invoice date
                .linesJson(linesJson)
                .cashierName("cashier")
                .createdAt(Instant.now())
                .build();
        invoice.assignScope(tenantId, companyId);
        return invoiceRepository.save(invoice);
    }

    private String resolveFiscalYear(LocalDate invoiceDate) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(invoiceDate);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            return "AD" + invoiceDate.getYear();
        }
    }
}
