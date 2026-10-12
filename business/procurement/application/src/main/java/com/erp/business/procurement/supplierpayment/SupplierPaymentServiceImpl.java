package com.erp.business.procurement.supplierpayment;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.dto.CreatePaymentRequest;
import com.erp.business.procurement.supplierpayment.dto.SupplierPaymentResponse;
import com.erp.business.procurement.supplierpayment.infrastructure.persistence.SupplierPaymentRepository;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Records supplier payments and settles them against their invoices.
 *
 * <p><strong>The invoice is loaded under a write lock.</strong> Two treasury clerks
 * paying the same invoice at the same moment would both read the same balance and both
 * be accepted, leaving it over-settled. The lock makes the second request wait, re-read
 * the reduced balance, and be refused if it no longer covers the amount.
 *
 * <p><strong>The invoice is the single writer of the balance.</strong> This service does
 * not decrement anything itself; it asks the invoice to record the payment and lets the
 * invoice re-derive both {@code paidAmount} and its status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private static final String DOCUMENT_TYPE = "SUPPLIER_PAYMENT";
    private static final String PREFIX = "PAY-";
    private static final int NUMBER_WIDTH = 6;

    private final SupplierPaymentRepository paymentRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final DocumentSequenceRepository sequenceRepository;

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_CREATE)
    public SupplierPaymentResponse pay(Long tenantId, Long companyId,
                                       CreatePaymentRequest request, String actor) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        PurchaseInvoice invoice = invoiceRepository
                .findByIdForUpdate(tenantId, companyId, request.purchaseInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase invoice " + request.purchaseInvoiceId() + " was not found"));

        try {
            invoice.recordPayment(request.amount());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }

        // Resolved once so the document date and the numbering period cannot disagree.
        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();

        SupplierPayment payment = SupplierPayment.post(
                allocateNumber(tenantId, companyId, paymentDate),
                paymentDate,
                request.amount(),
                request.paymentMethod(),
                request.referenceNo(),
                request.paidFrom(),
                invoice,
                request.notes(),
                actor);
        payment.assignScope(tenantId, companyId);

        // The invoice must reach the database with its reduced balance, and so must the
        // payment that explains why. If either fails both roll back together.
        invoiceRepository.saveAndFlush(invoice);
        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public SupplierPaymentResponse getById(Long tenantId, Long companyId, Long id) {
        return toResponse(paymentRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier payment " + id + " was not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public Page<SupplierPaymentResponse> findAll(Long tenantId, Long companyId, Pageable pageable) {
        return paymentRepository.findInScope(tenantId, companyId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public Page<SupplierPaymentResponse> findByInvoice(Long tenantId, Long companyId, Long invoiceId,
                                                      Pageable pageable) {
        return paymentRepository.findByInvoice(tenantId, companyId, invoiceId, pageable)
                .map(this::toResponse);
    }

    /**
     * Allocates the next payment number for the payment's own fiscal period, taking the same
     * pessimistic write lock the other document sequences use.
     */
    private String allocateNumber(Long tenantId, Long companyId, LocalDate paymentDate) {
        String periodKey = fiscalPeriodKey(paymentDate);
        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, DOCUMENT_TYPE, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, DOCUMENT_TYPE, periodKey,
                                highestIssuedNumber(tenantId, companyId))));

        return sequence.format(sequence.allocate(), PREFIX, NUMBER_WIDTH);
    }

    /**
     * Fiscal-year period key, matching the other document series so all of them reset at
     * the same boundary.
     */
    private String fiscalPeriodKey(LocalDate date) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(date);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.debug("Date {} is outside the published Bikram Sambat calendar; "
                    + "numbering against its Gregorian year instead", date);
            return "AD" + date.getYear();
        }
    }

    private long highestIssuedNumber(Long tenantId, Long companyId) {
        Long highest = paymentRepository.findHighestIssuedNumber(
                tenantId, companyId, PREFIX, PREFIX.length());
        return highest == null ? 0L : highest;
    }

    private SupplierPaymentResponse toResponse(SupplierPayment payment) {
        return new SupplierPaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getTenantId(),
                payment.getCompanyId(),
                payment.getBranchId(),
                payment.getFiscalYearId(),
                payment.getPurchaseInvoiceId(),
                payment.getInvoiceNumber(),
                payment.getPaymentDate(),
                payment.getSupplierId(),
                payment.getSupplierCode(),
                payment.getSupplierName(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getReferenceNo(),
                payment.getPaidFrom(),
                payment.getStatus(),
                payment.getNotes(),
                payment.getPostedAt(),
                payment.getPostedBy(),
                payment.getCancellationReason(),
                payment.getCreatedAt());
    }
}
