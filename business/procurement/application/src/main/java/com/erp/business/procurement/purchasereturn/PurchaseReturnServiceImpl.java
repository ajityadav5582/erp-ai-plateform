package com.erp.business.procurement.purchasereturn;

import com.erp.business.inventory.interfaces.stock.StockMovementBatchResponse;
import com.erp.business.inventory.interfaces.stock.StockMovementLineRequest;
import com.erp.business.inventory.interfaces.stock.StockMovementService;
import com.erp.business.inventory.interfaces.stock.StockReceiptRequest;
import com.erp.business.inventory.interfaces.warehouse.WarehouseService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceLine;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturn;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnLine;
import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnStatus;
import com.erp.business.procurement.purchasereturn.dto.CreatePurchaseReturnRequest;
import com.erp.business.procurement.purchasereturn.dto.PurchaseReturnResponse;
import com.erp.business.procurement.purchasereturn.dto.ReturnDecisionRequest;
import com.erp.business.procurement.purchasereturn.dto.ReturnableLineResponse;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnLineRepository;
import com.erp.business.procurement.purchasereturn.infrastructure.persistence.PurchaseReturnRepository;
import com.erp.business.procurement.shared.domain.DocumentSequence;
import com.erp.business.procurement.shared.infrastructure.persistence.DocumentSequenceRepository;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.business.procurement.supplierpayment.domain.PaymentMethod;
import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus;
import com.erp.business.procurement.supplierpayment.infrastructure.persistence.SupplierPaymentRepository;
import com.erp.platform.business.context.BusinessContextAccessor;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.common.nepali.date.NepaliDate;
import com.erp.platform.common.nepali.date.NepaliDateConverter;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PurchaseReturnServiceImpl implements PurchaseReturnService {

    private static final String DOCUMENT_TYPE = "PURCHASE_RETURN";
    private static final String PREFIX = "DN-";
    private static final int NUMBER_WIDTH = 5;

    private final PurchaseReturnRepository returnRepository;
    private final PurchaseReturnLineRepository returnLineRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final StockMovementService stockMovementService;
    private final WarehouseService warehouseService;
    private final PurchaseReturnMapper returnMapper;
    private final BusinessContextAccessor context;
    private final SupplierPaymentRepository supplierPaymentRepository;

    public PurchaseReturnServiceImpl(
            PurchaseReturnRepository returnRepository,
            PurchaseReturnLineRepository returnLineRepository,
            PurchaseInvoiceRepository invoiceRepository,
            GoodsReceiptRepository receiptRepository,
            DocumentSequenceRepository sequenceRepository,
            StockMovementService stockMovementService,
            WarehouseService warehouseService,
            PurchaseReturnMapper returnMapper,
            BusinessContextAccessor context,
            SupplierPaymentRepository supplierPaymentRepository
    ) {
        this.returnRepository = returnRepository;
        this.returnLineRepository = returnLineRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.sequenceRepository = sequenceRepository;
        this.stockMovementService = stockMovementService;
        this.warehouseService = warehouseService;
        this.returnMapper = returnMapper;
        this.context = context;
        this.supplierPaymentRepository = supplierPaymentRepository;
    }

    @Override
    @Transactional
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_CREATE, ProcurementPermissions.PURCHASE_INVOICE_CREATE}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse createReturn(Long tenantId, Long companyId, CreatePurchaseReturnRequest request) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("Tenant and company scope are required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ValidationException("A valid return reason is legally required under Nepal VAT Rule 23");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ValidationException("Purchase return must contain at least one line item");
        }

        PurchaseInvoice invoice = invoiceRepository
                .findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, request.purchaseInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase invoice " + request.purchaseInvoiceId() + " was not found"));

        if (invoice.getStatus() == PurchaseInvoiceStatus.CANCELLED) {
            throw new ValidationException("Cannot create a purchase return against cancelled invoice " + invoice.getInvoiceNumber());
        }

        // Resolve warehouse
        Long resolvedWarehouseId = request.warehouseId();
        String resolvedWarehouseCode = null;
        String resolvedWarehouseName = null;

        if (resolvedWarehouseId == null && invoice.getGoodsReceiptId() != null) {
            GoodsReceipt receipt = receiptRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, invoice.getGoodsReceiptId()).orElse(null);
            if (receipt != null) {
                resolvedWarehouseId = receipt.getWarehouseId();
                resolvedWarehouseCode = receipt.getWarehouseCode();
                resolvedWarehouseName = receipt.getWarehouseName();
            }
        }

        if (resolvedWarehouseId != null && (resolvedWarehouseCode == null || resolvedWarehouseName == null)) {
            WarehouseService.WarehouseResponse wh = warehouseService.findById(resolvedWarehouseId).orElse(null);
            if (wh != null) {
                resolvedWarehouseCode = wh.getCode();
                resolvedWarehouseName = wh.getName();
            }
        }

        if (resolvedWarehouseId == null) {
            throw new ValidationException("A valid warehouse is required to dispatch returned goods");
        }

        LocalDate returnDate = request.returnDate() != null ? request.returnDate() : LocalDate.now();
        String returnDateNp = null;
        try {
            returnDateNp = NepaliDateConverter.fromAd(returnDate).toString();
        } catch (RuntimeException ex) {
            log.debug("Return date outside Bikram Sambat calendar: {}", returnDate);
        }

        String debitNoteNumber = allocateNumber(tenantId, companyId, returnDate);

        PurchaseReturn purchaseReturn = PurchaseReturn.draft(
                debitNoteNumber,
                invoice,
                resolvedWarehouseId,
                resolvedWarehouseCode,
                resolvedWarehouseName,
                returnDate,
                returnDateNp,
                request.reason(),
                request.notes()
        );
        purchaseReturn.assignScope(tenantId, companyId);

        // Map existing invoice lines
        Map<Long, PurchaseInvoiceLine> invoiceLinesById = new HashMap<>();
        for (PurchaseInvoiceLine il : invoice.getLines()) {
            invoiceLinesById.put(il.getId(), il);
        }

        // Map returned quantities previously recorded
        Map<Long, BigDecimal> alreadyReturnedByLineId = new HashMap<>();
        for (Object[] row : returnLineRepository.sumReturnedQuantitiesByInvoiceId(tenantId, companyId, invoice.getId())) {
            if (row[0] != null) {
                alreadyReturnedByLineId.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
            }
        }

        int lineNumber = 1;
        for (CreatePurchaseReturnRequest.LineRequest lr : request.lines()) {
            PurchaseInvoiceLine invLine = null;
            if (lr.purchaseInvoiceLineId() != null) {
                invLine = invoiceLinesById.get(lr.purchaseInvoiceLineId());
            }
            if (invLine == null) {
                // Try matching by item ID
                invLine = invoice.getLines().stream()
                        .filter(l -> l.getItemId().equals(lr.itemId()))
                        .findFirst()
                        .orElse(null);
            }

            if (invLine == null) {
                throw new ValidationException("Item ID " + lr.itemId() + " does not belong to invoice " + invoice.getInvoiceNumber());
            }

            BigDecimal invoicedQty = invLine.getQuantity();
            BigDecimal priorReturned = alreadyReturnedByLineId.getOrDefault(invLine.getId(), BigDecimal.ZERO);
            BigDecimal maxReturnable = invoicedQty.subtract(priorReturned).max(BigDecimal.ZERO);

            if (lr.returnQuantity().compareTo(maxReturnable) > 0) {
                throw new ValidationException("Return quantity " + lr.returnQuantity()
                        + " for item " + invLine.getItemName()
                        + " exceeds remaining returnable quantity (" + maxReturnable + ")");
            }

            BigDecimal unitPrice = lr.unitPrice() != null ? lr.unitPrice() : invLine.getUnitPrice();
            PurchaseTaxCategory taxCategory = lr.taxCategory() != null ? lr.taxCategory() : (invLine.getTaxCategory() != null ? invLine.getTaxCategory() : PurchaseTaxCategory.TAXABLE);
            BigDecimal taxRate = lr.taxRate() != null ? lr.taxRate() : (invLine.getTaxRate() != null ? invLine.getTaxRate() : new BigDecimal("13.00"));

            PurchaseReturnLine returnLine = PurchaseReturnLine.create(
                    lineNumber++,
                    invLine.getId(),
                    invLine.getItemId(),
                    invLine.getItemCode(),
                    invLine.getItemName(),
                    invLine.getUomCode(),
                    lr.returnQuantity(),
                    unitPrice,
                    taxCategory,
                    taxRate,
                    lr.remarks()
            );

            purchaseReturn.addLine(returnLine);
        }

        PurchaseReturn saved = returnRepository.save(purchaseReturn);
        return returnMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse getReturnById(Long tenantId, Long companyId, Long id) {
        return returnMapper.toResponse(loadReturnWithLines(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse getReturnByDebitNoteNumber(Long tenantId, Long companyId, String debitNoteNumber) {
        PurchaseReturn r = returnRepository.findByTenantIdAndCompanyIdAndDebitNoteNumber(tenantId, companyId, debitNoteNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Debit note " + debitNoteNumber + " was not found"));
        return returnMapper.toResponse(r);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public Page<PurchaseReturnResponse> listReturns(Long tenantId, Long companyId, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public Page<PurchaseReturnResponse> listReturnsByStatus(Long tenantId, Long companyId, PurchaseReturnStatus status, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public Page<PurchaseReturnResponse> listReturnsByInvoice(Long tenantId, Long companyId, Long invoiceId, Pageable pageable) {
        return returnRepository.findByTenantIdAndCompanyIdAndPurchaseInvoiceId(tenantId, companyId, invoiceId, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public Page<PurchaseReturnResponse> searchReturns(Long tenantId, Long companyId, String term, Pageable pageable) {
        return returnRepository.searchByTerm(tenantId, companyId, term, pageable)
                .map(returnMapper::toResponse);
    }

    @Override
    @Transactional
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_CREATE, ProcurementPermissions.PURCHASE_INVOICE_CREATE}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse approveReturn(Long tenantId, Long companyId, Long id) {
        PurchaseReturn purchaseReturn = loadReturn(tenantId, companyId, id);
        purchaseReturn.approve(currentUser());
        return returnMapper.toResponse(returnRepository.save(purchaseReturn));
    }

    @Override
    @Transactional
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_CREATE, ProcurementPermissions.PURCHASE_INVOICE_CREATE}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse postReturn(Long tenantId, Long companyId, Long id) {
        PurchaseReturn purchaseReturn = returnRepository.findByIdForUpdate(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return " + id + " was not found"));

        if (purchaseReturn.getStatus() == PurchaseReturnStatus.POSTED) {
            return returnMapper.toResponse(purchaseReturn);
        }

        purchaseReturn.post(currentUser());

        // Deduct payable balance on source invoice
        PurchaseInvoice invoice = invoiceRepository.findByIdForUpdate(tenantId, companyId, purchaseReturn.getPurchaseInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase invoice " + purchaseReturn.getPurchaseInvoiceId() + " was not found"));

        BigDecimal previousBalance = invoice.getBalanceAmount();
        BigDecimal debitNoteTotal = purchaseReturn.getTotalAmount();
        invoice.recordDebitNote(debitNoteTotal);
        invoiceRepository.save(invoice);

        // If the debit note exceeds the outstanding invoice balance,
        // book the excess amount as an unallocated advance credit with the supplier.
        BigDecimal excess = debitNoteTotal.subtract(previousBalance);
        if (excess.compareTo(BigDecimal.ZERO) > 0) {
            String advPaymentNumber = "ADV-DN-" + purchaseReturn.getDebitNoteNumber();
            SupplierPayment advanceCredit = SupplierPayment.builder()
                    .paymentNumber(advPaymentNumber)
                    .paymentDate(purchaseReturn.getReturnDate() != null ? purchaseReturn.getReturnDate() : LocalDate.now())
                    .supplierId(purchaseReturn.getSupplierId())
                    .supplierCode(purchaseReturn.getSupplierCode())
                    .supplierName(purchaseReturn.getSupplierName())
                    .amount(excess)
                    .unallocatedAmount(excess)
                    .paymentMethod(PaymentMethod.OTHER)
                    .referenceNo("DN-" + purchaseReturn.getDebitNoteNumber())
                    .currency(purchaseReturn.getCurrency())
                    .status(SupplierPaymentStatus.POSTED)
                    .notes("Automatic Debit Note credit from Purchase Return " + purchaseReturn.getDebitNoteNumber()
                            + " exceeding settled invoice " + invoice.getInvoiceNumber())
                    .postedAt(Instant.now())
                    .postedBy(currentUser())
                    .build();
            advanceCredit.assignScope(tenantId, companyId);
            supplierPaymentRepository.save(advanceCredit);
            log.info("Created supplier advance credit {} of {} {} from Debit Note {}",
                    advPaymentNumber, excess, purchaseReturn.getCurrency(), purchaseReturn.getDebitNoteNumber());
        }

        PurchaseReturn saved = returnRepository.save(purchaseReturn);

        // Land stock outbound movement in Inventory (postIssue)
        StockReceiptRequest batch = stockBatch(saved, saved.getPostedAt(),
                "Purchase Return / Debit Note " + saved.getDebitNoteNumber() + " against Invoice " + saved.getInvoiceNumber());

        postStockAndUpdateStatus(tenantId, companyId, saved.getId(), batch);

        return returnMapper.toResponse(loadReturnWithLines(tenantId, companyId, saved.getId()));
    }

    @Override
    @Transactional
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_CANCEL, ProcurementPermissions.PURCHASE_INVOICE_CANCEL}, logical = RequiresPermission.LogicalOperator.OR)
    public PurchaseReturnResponse cancelReturn(Long tenantId, Long companyId, Long id, ReturnDecisionRequest request) {
        PurchaseReturn purchaseReturn = returnRepository.findByIdForUpdate(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return " + id + " was not found"));

        boolean wasPosted = purchaseReturn.getStatus() == PurchaseReturnStatus.POSTED;
        purchaseReturn.cancel(currentUser(), request.reason());

        if (wasPosted) {
            // Reverse AP adjustment on invoice
            invoiceRepository.findByIdForUpdate(tenantId, companyId, purchaseReturn.getPurchaseInvoiceId())
                    .ifPresent(inv -> {
                        inv.reverseDebitNote(purchaseReturn.getTotalAmount());
                        invoiceRepository.save(inv);
                    });

            // Void any advance credit generated by this return
            supplierPaymentRepository.findByTenantIdAndCompanyIdAndReferenceNo(
                    tenantId, companyId, "DN-" + purchaseReturn.getDebitNoteNumber())
                    .ifPresent(creditAdv -> {
                        creditAdv.cancel(currentUser(), "Purchase return " + purchaseReturn.getDebitNoteNumber() + " was cancelled");
                        supplierPaymentRepository.save(creditAdv);
                        log.info("Cancelled supplier advance credit for Debit Note {}", purchaseReturn.getDebitNoteNumber());
                    });

            // Reverse stock movement if it was posted to inventory
            if (purchaseReturn.getStockPostStatus() == StockPostStatus.POSTED) {
                StockReceiptRequest batch = stockBatch(purchaseReturn, Instant.now(),
                        "Cancellation of Purchase Return " + purchaseReturn.getDebitNoteNumber() + ": " + request.reason());
                try {
                    stockMovementService.reverseIssue(batch);
                } catch (RuntimeException ex) {
                    log.warn("Failed to reverse stock issue for cancelled return {}: {}", id, ex.getMessage());
                }
            }
        }

        PurchaseReturn saved = returnRepository.save(purchaseReturn);
        return returnMapper.toResponse(saved);
    }

    @Override
    public PurchaseReturnResponse retryStockPost(Long tenantId, Long companyId, Long id) {
        PurchaseReturn purchaseReturn = loadReturnWithLines(tenantId, companyId, id);

        if (purchaseReturn.getStatus() != PurchaseReturnStatus.POSTED || purchaseReturn.getStockPostStatus() == StockPostStatus.POSTED) {
            return returnMapper.toResponse(purchaseReturn);
        }

        StockReceiptRequest batch = stockBatch(purchaseReturn, purchaseReturn.getPostedAt(),
                "Retry stock dispatch for Purchase Return " + purchaseReturn.getDebitNoteNumber());

        postStockAndUpdateStatus(tenantId, companyId, id, batch);

        return returnMapper.toResponse(loadReturnWithLines(tenantId, companyId, id));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(value = {ProcurementPermissions.PURCHASE_RETURN_VIEW, ProcurementPermissions.PURCHASE_INVOICE_VIEW}, logical = RequiresPermission.LogicalOperator.OR)
    public List<ReturnableLineResponse> getReturnableLinesForInvoice(Long tenantId, Long companyId, Long invoiceId) {
        PurchaseInvoice invoice = invoiceRepository.findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase invoice " + invoiceId + " was not found"));

        Map<Long, BigDecimal> alreadyReturnedByLineId = new HashMap<>();
        for (Object[] row : returnLineRepository.sumReturnedQuantitiesByInvoiceId(tenantId, companyId, invoiceId)) {
            if (row[0] != null) {
                alreadyReturnedByLineId.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
            }
        }

        List<ReturnableLineResponse> result = new ArrayList<>();
        for (PurchaseInvoiceLine line : invoice.getLines()) {
            BigDecimal alreadyReturned = alreadyReturnedByLineId.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal returnable = line.getQuantity().subtract(alreadyReturned).max(BigDecimal.ZERO);

            result.add(new ReturnableLineResponse(
                    line.getId(),
                    line.getLineNumber(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getUomCode(),
                    line.getQuantity(),
                    alreadyReturned,
                    returnable,
                    line.getUnitPrice(),
                    line.getTaxCategory(),
                    line.getTaxRate()
            ));
        }

        return result;
    }

    private PurchaseReturn loadReturn(Long tenantId, Long companyId, Long id) {
        return returnRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return " + id + " was not found"));
    }

    private PurchaseReturn loadReturnWithLines(Long tenantId, Long companyId, Long id) {
        return returnRepository.findWithLinesByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return " + id + " was not found"));
    }

    private StockReceiptRequest stockBatch(PurchaseReturn purchaseReturn, Instant movementDate, String notes) {
        List<StockMovementLineRequest> lines = new ArrayList<>();
        for (PurchaseReturnLine line : purchaseReturn.getLines()) {
            String uomCode = line.getUomCode() == null ? null : line.getUomCode().trim().toUpperCase();
            lines.add(new StockMovementLineRequest(
                    purchaseReturn.getWarehouseId(),
                    line.getItemId(),
                    uomCode,
                    line.getReturnQuantity()));
        }
        return new StockReceiptRequest(
                "PURCHASE_RETURN",
                purchaseReturn.getId(),
                purchaseReturn.getDebitNoteNumber(),
                movementDate,
                purchaseReturn.getPostedBy(),
                notes,
                lines);
    }

    private void postStockAndUpdateStatus(Long tenantId, Long companyId, Long returnId, StockReceiptRequest batch) {
        try {
            stockMovementService.postIssue(batch);
            applyStockStatus(tenantId, companyId, returnId, StockPostStatus.POSTED, null);
        } catch (RuntimeException ex) {
            log.warn("Failed to post stock issue for purchase return {}: {}", returnId, ex.getMessage(), ex);
            applyStockStatus(tenantId, companyId, returnId, StockPostStatus.FAILED, ex.getMessage());
        }
    }

    private void applyStockStatus(Long tenantId, Long companyId, Long returnId, StockPostStatus status, String error) {
        returnRepository.findByTenantIdAndCompanyIdAndId(tenantId, companyId, returnId)
                .ifPresent(r -> {
                    if (status == StockPostStatus.POSTED) {
                        r.markStockPosted();
                    } else {
                        r.markStockFailed(error);
                    }
                    returnRepository.saveAndFlush(r);
                });
    }

    private String allocateNumber(Long tenantId, Long companyId, LocalDate returnDate) {
        String periodKey = fiscalPeriodKey(returnDate);
        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, DOCUMENT_TYPE, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, DOCUMENT_TYPE, periodKey,
                                highestIssuedNumber(tenantId, companyId))));

        return sequence.format(sequence.allocate(), PREFIX, NUMBER_WIDTH);
    }

    private String fiscalPeriodKey(LocalDate date) {
        try {
            NepaliDate bs = NepaliDateConverter.fromAd(date);
            int startYear = bs.month() >= 4 ? bs.year() : bs.year() - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.debug("Date {} is outside published Bikram Sambat calendar; using AD year", date);
            return "AD" + date.getYear();
        }
    }

    private long highestIssuedNumber(Long tenantId, Long companyId) {
        Long highest = returnRepository.findHighestIssuedNumber(
                tenantId, companyId, PREFIX, PREFIX.length());
        return highest == null ? 0L : highest;
    }

    private String currentUser() {
        return context != null && context.getUserId() != null ? context.getUserId() : "SYSTEM";
    }
}
