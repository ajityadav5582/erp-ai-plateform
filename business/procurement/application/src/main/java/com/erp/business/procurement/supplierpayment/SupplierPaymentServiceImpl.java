package com.erp.business.procurement.supplierpayment;

import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentAllocation;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus;
import com.erp.business.procurement.supplierpayment.dto.ApplyAdvanceRequest;
import com.erp.business.procurement.supplierpayment.dto.CreatePaymentRequest;
import com.erp.business.procurement.supplierpayment.dto.PaymentAllocationRequest;
import com.erp.business.procurement.supplierpayment.dto.PaymentAllocationResponse;
import com.erp.business.procurement.supplierpayment.dto.SupplierBalanceSummaryResponse;
import com.erp.business.procurement.supplierpayment.dto.SupplierPaymentResponse;
import com.erp.business.procurement.supplierpayment.dto.SupplierStatementLineResponse;
import com.erp.business.procurement.supplierpayment.dto.SupplierStatementResponse;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Records supplier payments and settles them against their invoices.
 *
 * <p><strong>Invoices are loaded under write locks in canonical order.</strong>
 * When settling multiple invoices, their IDs are sorted ascending before acquiring locks to
 * prevent database deadlocks. Two concurrent payments targeting overlapping sets of invoices
 * will always lock in the same sequence.
 *
 * <p><strong>The invoice is the single writer of the balance.</strong> This service does
 * not decrement anything itself; it asks each invoice to record its allocated payment
 * and lets the invoice re-derive both {@code paidAmount} and its status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private static final String DOCUMENT_TYPE = "SUPPLIER_PAYMENT";
    private static final String PREFIX = "PAY-";
    private static final int NUMBER_WIDTH = 6;
    private static final int MONEY_SCALE = 4;

    private final SupplierPaymentRepository paymentRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final SupplierService supplierService;

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_CREATE)
    public SupplierPaymentResponse pay(Long tenantId, Long companyId,
                                       CreatePaymentRequest request, String actor) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero");
        }
        if (request.paymentMethod() == null) {
            throw new ValidationException("A payment method is required");
        }

        boolean hasAllocations = request.allocations() != null && !request.allocations().isEmpty();

        if (hasAllocations) {
            return payMultiInvoice(tenantId, companyId, request, actor);
        } else if (request.purchaseInvoiceId() != null) {
            return paySingleInvoice(tenantId, companyId, request, actor);
        } else if (request.supplierId() != null) {
            return payAdvance(tenantId, companyId, request, actor);
        } else {
            throw new ValidationException("A supplier or purchase invoice is required to record a payment");
        }
    }

    /**
     * Advance payment flow (money paid upfront without invoice allocations).
     */
    private SupplierPaymentResponse payAdvance(Long tenantId, Long companyId,
                                               CreatePaymentRequest request, String actor) {
        SupplierService.SupplierResponse supplier = supplierService.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier " + request.supplierId() + " was not found"));

        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();
        String paymentNumber = allocateNumber(tenantId, companyId, paymentDate);

        SupplierPayment payment = SupplierPayment.postMultiInvoice(
                paymentNumber,
                paymentDate,
                request.amount(),
                request.amount(), // 100% advance credit
                request.paymentMethod(),
                request.referenceNo(),
                request.paidFrom(),
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                "NPR",
                null,
                null,
                request.notes() != null && !request.notes().isBlank()
                        ? request.notes().trim()
                        : "Supplier advance payment",
                actor
        );
        payment.assignScope(tenantId, companyId);
        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    /**
     * Legacy & quick single-invoice settlement flow.
     */
    private SupplierPaymentResponse paySingleInvoice(Long tenantId, Long companyId,
                                                     CreatePaymentRequest request, String actor) {
        PurchaseInvoice invoice = invoiceRepository
                .findByIdForUpdate(tenantId, companyId, request.purchaseInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase invoice " + request.purchaseInvoiceId() + " was not found"));

        try {
            invoice.recordPayment(request.amount());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ValidationException(ex.getMessage());
        }

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

        invoiceRepository.saveAndFlush(invoice);
        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    /**
     * Multi-invoice settlement flow with deadlock-free canonical lock ordering.
     */
    private SupplierPaymentResponse payMultiInvoice(Long tenantId, Long companyId,
                                                    CreatePaymentRequest request, String actor) {
        List<PaymentAllocationRequest> allocationRequests = request.allocations();

        // 1. Guard against duplicate invoice allocation lines
        Set<Long> seenInvoiceIds = new HashSet<>();
        for (PaymentAllocationRequest allocReq : allocationRequests) {
            if (allocReq.purchaseInvoiceId() == null) {
                throw new ValidationException("Each allocation must specify a purchase invoice ID");
            }
            if (!seenInvoiceIds.add(allocReq.purchaseInvoiceId())) {
                throw new ValidationException("Duplicate invoice allocation for invoice ID " + allocReq.purchaseInvoiceId());
            }
        }

        // 2. Canonical lock ordering: sort invoice IDs ascending to avoid deadlocks
        List<Long> sortedInvoiceIds = allocationRequests.stream()
                .map(PaymentAllocationRequest::purchaseInvoiceId)
                .sorted()
                .toList();

        List<PurchaseInvoice> lockedInvoices = invoiceRepository
                .findAllByIdForUpdate(tenantId, companyId, sortedInvoiceIds);

        Map<Long, PurchaseInvoice> invoiceMap = lockedInvoices.stream()
                .collect(Collectors.toMap(PurchaseInvoice::getId, Function.identity()));

        // Ensure all target invoices exist
        for (Long invoiceId : sortedInvoiceIds) {
            if (!invoiceMap.containsKey(invoiceId)) {
                throw new ResourceNotFoundException("Purchase invoice " + invoiceId + " was not found");
            }
        }

        // 3. Validate supplier consistency
        PurchaseInvoice firstInvoice = lockedInvoices.get(0);
        Long supplierId = request.supplierId() != null ? request.supplierId() : firstInvoice.getSupplierId();
        String supplierCode = firstInvoice.getSupplierCode();
        String supplierName = firstInvoice.getSupplierName();
        String currency = firstInvoice.getCurrency();
        Long branchId = firstInvoice.getBranchId();
        Long fiscalYearId = firstInvoice.getFiscalYearId();

        for (PurchaseInvoice inv : lockedInvoices) {
            if (!supplierId.equals(inv.getSupplierId())) {
                throw new ValidationException("Invoice " + inv.getInvoiceNumber()
                        + " does not belong to the selected supplier");
            }
            if (!currency.equalsIgnoreCase(inv.getCurrency())) {
                throw new ValidationException("Cannot settle invoices in different currencies within one payment: expected "
                        + currency + ", got " + inv.getCurrency() + " for invoice " + inv.getInvoiceNumber());
            }
        }

        // 4. Validate and apply allocations to each invoice
        BigDecimal totalAllocated = BigDecimal.ZERO;
        for (PaymentAllocationRequest allocReq : allocationRequests) {
            PurchaseInvoice invoice = invoiceMap.get(allocReq.purchaseInvoiceId());
            BigDecimal allocAmount = allocReq.allocatedAmount();

            if (allocAmount == null || allocAmount.signum() <= 0) {
                throw new ValidationException("Allocated amount for invoice "
                        + invoice.getInvoiceNumber() + " must be greater than zero");
            }

            try {
                invoice.recordPayment(allocAmount);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                throw new ValidationException("Invoice " + invoice.getInvoiceNumber() + ": " + ex.getMessage());
            }

            totalAllocated = totalAllocated.add(allocAmount);
        }

        // 5. Total payment must be >= total allocated
        if (request.amount().compareTo(totalAllocated) < 0) {
            throw new ValidationException(String.format(
                    "Total payment amount (%s) cannot be less than total allocated to invoices (%s)",
                    request.amount(), totalAllocated));
        }

        BigDecimal unallocatedAmount = request.amount().subtract(totalAllocated)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        LocalDate paymentDate = request.paymentDate() != null ? request.paymentDate() : LocalDate.now();

        SupplierPayment payment = SupplierPayment.postMultiInvoice(
                allocateNumber(tenantId, companyId, paymentDate),
                paymentDate,
                request.amount(),
                unallocatedAmount,
                request.paymentMethod(),
                request.referenceNo(),
                request.paidFrom(),
                supplierId,
                supplierCode,
                supplierName,
                currency,
                branchId,
                fiscalYearId,
                request.notes(),
                actor
        );
        payment.assignScope(tenantId, companyId);

        for (PaymentAllocationRequest allocReq : allocationRequests) {
            PurchaseInvoice invoice = invoiceMap.get(allocReq.purchaseInvoiceId());
            SupplierPaymentAllocation allocation = SupplierPaymentAllocation.of(
                    payment,
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    allocReq.allocatedAmount(),
                    allocReq.discountAmount(),
                    allocReq.notes()
            );
            payment.addAllocation(allocation);
        }

        // Save all updated invoices and the payment
        invoiceRepository.saveAllAndFlush(lockedInvoices);
        return toResponse(paymentRepository.saveAndFlush(payment));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public SupplierPaymentResponse getById(Long tenantId, Long companyId, Long id) {
        return toResponse(paymentRepository
                .findWithAllocationsById(tenantId, companyId, id)
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

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public List<SupplierPaymentResponse> findAvailableAdvances(Long tenantId, Long companyId, Long supplierId) {
        if (tenantId == null || companyId == null || supplierId == null) {
            return Collections.emptyList();
        }
        return paymentRepository.findAvailableAdvances(tenantId, companyId, supplierId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_CREATE)
    public SupplierPaymentResponse applyAdvance(Long tenantId, Long companyId,
                                                ApplyAdvanceRequest request, String actor) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }
        if (request.amountToApply() == null || request.amountToApply().signum() <= 0) {
            throw new ValidationException("Amount to apply must be greater than zero");
        }

        SupplierPayment payment = paymentRepository
                .findByIdForUpdate(tenantId, companyId, request.advancePaymentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Advance payment " + request.advancePaymentId() + " was not found"));

        if (payment.getStatus() != SupplierPaymentStatus.POSTED) {
            throw new ValidationException("Cannot apply advance from a " + payment.getStatus() + " payment");
        }

        if (payment.getUnallocatedAmount().compareTo(request.amountToApply()) < 0) {
            throw new ValidationException(String.format(
                    "Requested amount (%s) exceeds available advance balance (%s)",
                    request.amountToApply(), payment.getUnallocatedAmount()));
        }

        PurchaseInvoice invoice = invoiceRepository
                .findByIdForUpdate(tenantId, companyId, request.purchaseInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase invoice " + request.purchaseInvoiceId() + " was not found"));

        if (!payment.getSupplierId().equals(invoice.getSupplierId())) {
            throw new ValidationException(String.format(
                    "Advance payment belongs to supplier %s but invoice belongs to supplier %s",
                    payment.getSupplierName(), invoice.getSupplierName()));
        }

        if (!payment.getCurrency().equalsIgnoreCase(invoice.getCurrency())) {
            throw new ValidationException(String.format(
                    "Currency mismatch: advance is in %s but invoice is in %s",
                    payment.getCurrency(), invoice.getCurrency()));
        }

        try {
            invoice.recordPayment(request.amountToApply());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ValidationException("Invoice " + invoice.getInvoiceNumber() + ": " + ex.getMessage());
        }

        payment.deductUnallocated(request.amountToApply());

        SupplierPaymentAllocation allocation = SupplierPaymentAllocation.of(
                payment,
                invoice.getId(),
                invoice.getInvoiceNumber(),
                request.amountToApply(),
                BigDecimal.ZERO,
                request.notes() != null && !request.notes().isBlank()
                        ? request.notes().trim()
                        : "Advance credit applied to invoice " + invoice.getInvoiceNumber()
        );
        payment.addAllocation(allocation);

        invoiceRepository.saveAndFlush(invoice);
        return toResponse(paymentRepository.saveAndFlush(payment));
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
        List<PaymentAllocationResponse> allocationResponses = new ArrayList<>();
        if (payment.getAllocations() != null) {
            for (SupplierPaymentAllocation a : payment.getAllocations()) {
                allocationResponses.add(new PaymentAllocationResponse(
                        a.getId(),
                        a.getPurchaseInvoiceId(),
                        a.getInvoiceNumber(),
                        a.getAllocatedAmount(),
                        a.getDiscountAmount(),
                        a.getNotes()
                ));
            }
        }

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
                payment.getUnallocatedAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getReferenceNo(),
                payment.getPaidFrom(),
                payment.getStatus(),
                payment.getNotes(),
                payment.getPostedAt(),
                payment.getPostedBy(),
                payment.getCancellationReason(),
                payment.getCreatedAt(),
                allocationResponses);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public SupplierBalanceSummaryResponse getSupplierBalanceSummary(Long tenantId, Long companyId, Long supplierId) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("Tenant and company scope are required");
        }
        if (supplierId == null) {
            throw new ValidationException("Supplier ID is required");
        }

        List<PurchaseInvoice> unpaidInvoices = invoiceRepository.findOutstandingBySupplier(
                tenantId, companyId, supplierId, Pageable.unpaged()).getContent();

        BigDecimal totalOutstandingPayable = unpaidInvoices.stream()
                .map(PurchaseInvoice::getBalanceAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SupplierPayment> advances = paymentRepository.findAvailableAdvances(
                tenantId, companyId, supplierId);

        BigDecimal totalAvailableAdvance = advances.stream()
                .map(SupplierPayment::getUnallocatedAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = !unpaidInvoices.isEmpty()
                ? unpaidInvoices.get(0).getCurrency()
                : (!advances.isEmpty() ? advances.get(0).getCurrency() : "NPR");

        return new SupplierBalanceSummaryResponse(
                supplierId,
                totalOutstandingPayable,
                totalAvailableAdvance,
                unpaidInvoices.size(),
                advances.size(),
                currency
        );
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.SUPPLIER_PAYMENT_VIEW)
    public SupplierStatementResponse getSupplierStatement(Long tenantId, Long companyId, Long supplierId,
                                                          LocalDate fromDate, LocalDate toDate) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("Tenant and company scope are required");
        }
        if (supplierId == null) {
            throw new ValidationException("Supplier ID is required");
        }

        SupplierService.SupplierResponse supplier = supplierService.findById(supplierId)
                .orElseThrow(() -> new ValidationException("Supplier not found: " + supplierId));

        // 1. Fetch all financial records for this supplier
        List<PurchaseInvoice> invoices = invoiceRepository.findByTenantIdAndCompanyIdAndSupplierId(
                tenantId, companyId, supplierId);
        List<SupplierPayment> payments = paymentRepository.findByTenantIdAndCompanyIdAndSupplierId(
                tenantId, companyId, supplierId);
        List<PurchaseReturn> returns = purchaseReturnRepository.findByTenantIdAndCompanyIdAndSupplierId(
                tenantId, companyId, supplierId);

        // 2. Build flat list of all entries with date for sorting
        record RawEntry(LocalDate date, String type, Long id, String docNo, String refNo, String desc,
                        BigDecimal debit, BigDecimal credit) {}

        List<RawEntry> allEntries = new ArrayList<>();

        for (PurchaseInvoice inv : invoices) {
            if (inv.getStatus() == PurchaseInvoiceStatus.CANCELLED) {
                continue;
            }
            LocalDate d = inv.getInvoiceDate() != null ? inv.getInvoiceDate() : inv.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            BigDecimal amount = inv.getNetPayableAmount() != null ? inv.getNetPayableAmount() : inv.getTotalAmount();
            String desc = "Purchase Invoice" + (inv.getPoNumber() != null ? " (PO: " + inv.getPoNumber() + ")" : "");
            allEntries.add(new RawEntry(d, "INVOICE", inv.getId(), inv.getInvoiceNumber(), inv.getInvoiceNumber(), desc, BigDecimal.ZERO, amount));
        }

        for (SupplierPayment pay : payments) {
            if (pay.getStatus() == SupplierPaymentStatus.CANCELLED) {
                continue;
            }
            LocalDate d = pay.getPaymentDate() != null ? pay.getPaymentDate() : pay.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            String desc = "Supplier Payment (" + pay.getPaymentMethod() + (pay.getPaidFrom() != null ? " via " + pay.getPaidFrom() : "") + ")";
            allEntries.add(new RawEntry(d, "PAYMENT", pay.getId(), pay.getPaymentNumber(), pay.getReferenceNo(), desc, pay.getAmount(), BigDecimal.ZERO));
        }

        for (PurchaseReturn ret : returns) {
            if (ret.getStatus() == PurchaseReturnStatus.CANCELLED || ret.getStatus() == PurchaseReturnStatus.DRAFT) {
                continue;
            }
            LocalDate d = ret.getReturnDate() != null ? ret.getReturnDate() : ret.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            String desc = "Debit Note / Purchase Return (against " + ret.getInvoiceNumber() + ")";
            allEntries.add(new RawEntry(d, "DEBIT_NOTE", ret.getId(), ret.getDebitNoteNumber(), ret.getInvoiceNumber(), desc, ret.getTotalAmount(), BigDecimal.ZERO));
        }

        // Sort chronologically: earliest date first; if same date, invoices before payments/returns
        allEntries.sort((a, b) -> {
            int dateCmp = a.date().compareTo(b.date());
            if (dateCmp != 0) return dateCmp;
            if ("INVOICE".equals(a.type()) && !"INVOICE".equals(b.type())) return -1;
            if (!"INVOICE".equals(a.type()) && "INVOICE".equals(b.type())) return 1;
            return a.id().compareTo(b.id());
        });

        // 3. Compute opening balance (all transactions before fromDate)
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal runningBalance = BigDecimal.ZERO;
        List<SupplierStatementLineResponse> statementLines = new ArrayList<>();
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalReturns = BigDecimal.ZERO;

        for (RawEntry entry : allEntries) {
            if (fromDate != null && entry.date().isBefore(fromDate)) {
                openingBalance = openingBalance.add(entry.credit()).subtract(entry.debit());
            } else if (toDate == null || !entry.date().isAfter(toDate)) {
                if (statementLines.isEmpty()) {
                    runningBalance = openingBalance;
                }
                runningBalance = runningBalance.add(entry.credit()).subtract(entry.debit());
                if ("INVOICE".equals(entry.type())) {
                    totalInvoiced = totalInvoiced.add(entry.credit());
                } else if ("PAYMENT".equals(entry.type())) {
                    totalPaid = totalPaid.add(entry.debit());
                } else if ("DEBIT_NOTE".equals(entry.type())) {
                    totalReturns = totalReturns.add(entry.debit());
                }

                statementLines.add(new SupplierStatementLineResponse(
                        entry.date(),
                        entry.type(),
                        entry.id(),
                        entry.docNo(),
                        entry.refNo(),
                        entry.desc(),
                        entry.debit().setScale(2, RoundingMode.HALF_UP),
                        entry.credit().setScale(2, RoundingMode.HALF_UP),
                        runningBalance.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        if (statementLines.isEmpty()) {
            runningBalance = openingBalance;
        }

        BigDecimal closingBalance = runningBalance.setScale(2, RoundingMode.HALF_UP);

        return new SupplierStatementResponse(
                supplierId,
                supplier.getCode(),
                supplier.getName(),
                "NPR",
                fromDate,
                toDate,
                openingBalance.setScale(2, RoundingMode.HALF_UP),
                totalInvoiced.setScale(2, RoundingMode.HALF_UP),
                totalPaid.setScale(2, RoundingMode.HALF_UP),
                totalReturns.setScale(2, RoundingMode.HALF_UP),
                closingBalance,
                statementLines
        );
    }
}
