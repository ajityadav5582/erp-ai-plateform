package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.goodsreceipt.GoodsReceiptService;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.dto.CreateGoodsReceiptRequest;
import com.erp.business.procurement.goodsreceipt.dto.GoodsReceiptResponse;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceLine;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceType;
import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.dto.FastMatchPoRequest;
import com.erp.business.procurement.purchaseinvoice.dto.PurchaseInvoiceResponse;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
import com.erp.business.inventory.interfaces.item.ItemService;
import com.erp.business.inventory.interfaces.supplier.SupplierService;
import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.erp.business.procurement.shared.security.ProcurementPermissions;
import com.erp.platform.common.exception.ResourceNotFoundException;
import com.erp.platform.common.exception.ValidationException;
import com.erp.platform.security.annotation.RequiresPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Records supplier invoices and tracks the resulting payable.
 *
 * <p><strong>Only a posted receipt can be billed.</strong> Billing goods that have not
 * arrived would create a payable for nothing, and the receipt is the only document that
 * says they arrived.
 *
 * <p><strong>One invoice per delivery.</strong> A unique key on the receipt id holds it at
 * the database; the lookup below turns the loser into a readable error. Without both,
 * the same delivery could be billed twice and the payable would double for one set of
 * goods.
 *
 * <p><strong>No document number is allocated.</strong> The invoice carries the supplier's
 * own number, which is what the company is audited against. A company-internal number
 * would be a second identifier for the same document, and the two would eventually
 * disagree.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseInvoiceServiceImpl implements PurchaseInvoiceService {

    private final PurchaseInvoiceRepository invoiceRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final ItemService itemService;
    private final SupplierService supplierService;
    private final TaxRateService taxRateService;
    private final GoodsReceiptService goodsReceiptService;

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_CREATE)
    public PurchaseInvoiceResponse create(Long tenantId, Long companyId,
                                          CreateInvoiceRequest request) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        if (request.goodsReceiptId() != null) {
            return createFromReceipt(tenantId, companyId, request);
        } else {
            return createDirectInvoice(tenantId, companyId, request);
        }
    }

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_CREATE)
    public PurchaseInvoiceResponse fastMatchPurchaseOrder(Long tenantId, Long companyId,
                                                          FastMatchPoRequest request, String actor) {
        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

        // 1. Record Goods Receipt against the PO (receives all outstanding lines in full by default)
        CreateGoodsReceiptRequest grnRequest = new CreateGoodsReceiptRequest(
                request.purchaseOrderId(),
                request.invoiceDate() != null ? request.invoiceDate() : java.time.LocalDate.now(),
                request.warehouseId(),
                request.deliveryChallanNo(),
                request.notes()
        );

        GoodsReceiptResponse grn = goodsReceiptService.record(tenantId, companyId, grnRequest, actor);

        // 2. Map receipt lines into invoice lines
        List<CreateInvoiceRequest.Line> invoiceLines = grn.lines().stream()
                .map(l -> new CreateInvoiceRequest.Line(
                        l.id(),
                        l.receivedQuantity(),
                        l.unitPrice(),
                        BigDecimal.ZERO,
                        null,
                        null,
                        null
                ))
                .toList();

        // 3. Create the 3-way matched Purchase Invoice linked to the posted Goods Receipt
        CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest(
                grn.id(),
                PurchaseInvoiceType.GOODS_RECEIPT,
                grn.supplierId(),
                null,
                null,
                null,
                request.invoiceNumber(),
                request.invoiceDate() != null ? request.invoiceDate() : java.time.LocalDate.now(),
                request.dueDate(),
                invoiceLines,
                request.notes()
        );

        return createFromReceipt(tenantId, companyId, invoiceRequest);
    }

    private PurchaseInvoiceResponse createFromReceipt(Long tenantId, Long companyId,
                                                      CreateInvoiceRequest request) {
        GoodsReceipt receipt = receiptRepository
                .findByIdForUpdate(tenantId, companyId, request.goodsReceiptId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Goods receipt " + request.goodsReceiptId() + " was not found"));

        if (receipt.getStatus() != GoodsReceiptStatus.POSTED) {
            throw new ValidationException(
                    "Goods receipt " + receipt.getGrnNumber() + " is " + receipt.getStatus()
                            + " and cannot be invoiced");
        }

        invoiceRepository
                .findByTenantIdAndCompanyIdAndGoodsReceiptId(
                        tenantId, companyId, receipt.getId())
                .ifPresent(existing -> {
                    throw new ValidationException(
                            "Goods receipt " + receipt.getGrnNumber()
                                    + " has already been invoiced as " + existing.getInvoiceNumber());
                });

        // The same supplier invoice must not be entered twice, or one bill becomes two
        // payables. Checked here so the caller is told which number clashed rather than
        // meeting a database constraint violation.
        if (invoiceRepository.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(
                tenantId, companyId, receipt.getSupplierId(), request.invoiceNumber())) {
            throw new ValidationException(
                    "Supplier " + receipt.getSupplierCode() + " already has an invoice numbered "
                            + request.invoiceNumber());
        }

        PurchaseInvoice invoice = PurchaseInvoice.approved(
                request.invoiceNumber(),
                request.invoiceDate(),
                request.dueDate(),
                receipt,
                request.notes());
        invoice.assignScope(tenantId, companyId);
        invoice.configureTds(request.isTdsApplicable(), request.tdsRate());

        // Snapshot supplier VAT registration details at invoice creation time.
        // Historical invoices must preserve the supplier's VAT profile as it was
        // when the invoice was created, even if the supplier profile changes later.
        supplierService.findById(receipt.getSupplierId()).ifPresent(supplier -> {
            invoice.setSupplierVatSnapshot(
                    supplier.getVatRegistrationStatus(),
                    supplier.getVatRegistrationNumber(),
                    supplier.getTaxNumber());
        });

        int lineNumber = 1;
        List<Long> billedLines = new ArrayList<>();
        for (CreateInvoiceRequest.Line requested : request.lines()) {
            // A receipt line may be billed once per invoice. addLine only rejects a
            // repeated line number, so without this the same delivery could be billed
            // twice on one invoice.
            if (billedLines.contains(requested.goodsReceiptLineId())) {
                throw new ValidationException(
                        "Goods receipt line " + requested.goodsReceiptLineId()
                                + " is billed more than once on invoice " + request.invoiceNumber());
            }
            billedLines.add(requested.goodsReceiptLineId());

            invoice.addLine(buildLine(invoice, receipt, requested, lineNumber++));
        }

        if (invoice.getTotalAmount().signum() <= 0) {
            throw new ValidationException(
                    "Invoice " + request.invoiceNumber() + " has a total of zero and nothing to pay");
        }

        return toResponse(invoiceRepository.saveAndFlush(invoice));
    }

    private PurchaseInvoiceResponse createDirectInvoice(Long tenantId, Long companyId,
                                                        CreateInvoiceRequest request) {
        if (request.supplierId() == null) {
            throw new ValidationException("Supplier is required for direct invoice");
        }

        SupplierService.SupplierResponse supplier = supplierService.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier " + request.supplierId() + " was not found"));

        if (invoiceRepository.existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(
                tenantId, companyId, supplier.getId(), request.invoiceNumber())) {
            throw new ValidationException(
                    "Supplier " + supplier.getCode() + " already has an invoice numbered "
                            + request.invoiceNumber());
        }

        PurchaseInvoice invoice = PurchaseInvoice.direct(
                request.invoiceNumber(),
                request.invoiceDate(),
                request.dueDate(),
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                request.currency(),
                request.branchId(),
                request.fiscalYearId(),
                request.notes());
        invoice.assignScope(tenantId, companyId);
        invoice.configureTds(request.isTdsApplicable(), request.tdsRate());

        invoice.setSupplierVatSnapshot(
                supplier.getVatRegistrationStatus(),
                supplier.getVatRegistrationNumber(),
                supplier.getTaxNumber());

        int lineNumber = 1;
        for (CreateInvoiceRequest.Line requested : request.lines()) {
            invoice.addLine(buildDirectLine(invoice, requested, lineNumber++));
        }

        if (invoice.getTotalAmount().signum() <= 0) {
            throw new ValidationException(
                    "Invoice " + request.invoiceNumber() + " has a total of zero and nothing to pay");
        }

        return toResponse(invoiceRepository.saveAndFlush(invoice));
    }

    private PurchaseInvoiceLine buildDirectLine(PurchaseInvoice invoice,
                                                CreateInvoiceRequest.Line requested, int lineNumber) {
        if (requested.itemId() == null) {
            throw new ValidationException("Item is required for direct invoice line " + lineNumber);
        }

        ItemService.ItemResponse item = itemService.findById(requested.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item " + requested.itemId() + " was not found"));

        BigDecimal quantity = requested.quantity();
        if (quantity == null || quantity.signum() <= 0) {
            throw new ValidationException("Quantity must be greater than zero for line " + lineNumber);
        }

        BigDecimal unitPrice = requested.unitPrice();
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new ValidationException("Unit price cannot be negative for line " + lineNumber);
        }

        com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory category = requested.taxCategory();
        if (category == null && item.getTaxabilityType() != null) {
            category = switch (item.getTaxabilityType().toUpperCase()) {
                case "EXEMPT" -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.EXEMPT;
                case "ZERO_RATED" -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.ZERO_RATED;
                default -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE;
            };
        }
        if (category == null || category == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.UNCLASSIFIED) {
            category = com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE;
        }

        boolean isPanOnly = "PAN_ONLY".equalsIgnoreCase(invoice.getSupplierVatRegistrationStatus());
        if (isPanOnly) {
            if (requested.taxCategory() == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE
                    || (requested.taxRate() != null && requested.taxRate().compareTo(BigDecimal.ZERO) > 0)) {
                throw new ValidationException(
                        "Supplier " + invoice.getSupplierCode() + " is PAN-only (not registered for VAT). "
                                + "Under Nepal VAT Act 2052, VAT cannot be charged or claimed on invoices from PAN-only suppliers.");
            }
            category = com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.NON_TAXABLE;
        }

        BigDecimal taxRate = BigDecimal.ZERO;
        if (category == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE) {
            if (requested.taxRate() != null && requested.taxRate().signum() >= 0) {
                taxRate = requested.taxRate();
            } else {
                com.erp.business.inventory.interfaces.tax.TaxRateService.TaxRate tr =
                        taxRateService.findActive(invoice.getTenantId(), invoice.getCompanyId(), "VAT", invoice.getInvoiceDate());
                if (tr != null) {
                    taxRate = tr.rate();
                } else if (item.getVatRate() != null) {
                    taxRate = item.getVatRate();
                } else {
                    taxRate = new BigDecimal("13.00");
                }
            }
        }

        String itemCode = requested.itemCode() != null && !requested.itemCode().isBlank()
                ? requested.itemCode() : item.getSku();
        String itemName = requested.itemName() != null && !requested.itemName().isBlank()
                ? requested.itemName() : item.getName();
        String uomCode = requested.uomCode() != null && !requested.uomCode().isBlank()
                ? requested.uomCode() : (item.getUnitCode() != null ? item.getUnitCode() : (item.getUnitName() != null ? item.getUnitName() : "UNIT"));

        try {
            PurchaseInvoiceLine line = PurchaseInvoiceLine.direct(
                    lineNumber, item.getId(), itemCode, itemName, uomCode,
                    quantity, unitPrice, requested.discountPercentage(), taxRate, requested.description());
            line.calculateTax(category, unitPrice, quantity, taxRate,
                    item != null && Boolean.TRUE.equals(item.getIsVatInclusive()),
                    requested.discountPercentage() != null ? requested.discountPercentage() : BigDecimal.ZERO);
            return line;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "Line " + lineNumber + " of invoice " + invoice.getInvoiceNumber()
                            + " is not valid: " + ex.getMessage());
        }
    }

    /**
     * Turns a requested line into an invoice line.
     *
     * <p>Omitted quantity, price and tax default to what the receipt recorded, so a
     * faithful invoice needs nothing but the receipt line id. An omitted price is
     * deliberately taken from the receipt rather than refused: the common case is that
     * the supplier billed exactly the ordered price, and making the clerk re-key it adds
     * a chance to type a different number by mistake.
     */
    private PurchaseInvoiceLine buildLine(PurchaseInvoice invoice, GoodsReceipt receipt,
                                          CreateInvoiceRequest.Line requested, int lineNumber) {

        GoodsReceiptLine receiptLine = findReceiptLine(receipt, requested.goodsReceiptLineId());

        ItemService.ItemResponse item = itemService.findById(receiptLine.getItemId()).orElse(null);

        BigDecimal quantity = requested.quantity() != null
                ? requested.quantity() : receiptLine.getReceivedQuantity();
        BigDecimal unitPrice = requested.unitPrice() != null
                ? requested.unitPrice() : receiptLine.getUnitPrice();

        // 3-Way Match Verification: Billed quantity cannot exceed goods received quantity
        if (quantity.compareTo(receiptLine.getReceivedQuantity()) > 0) {
            throw new ValidationException(String.format(
                    "3-way match failure: Billed quantity (%s) exceeds received quantity (%s) for line [%s]",
                    quantity, receiptLine.getReceivedQuantity(), receiptLine.getItemCode()));
        }

        // 3-Way Match Verification: Price variance cannot exceed 5% tolerance against approved PO price
        BigDecimal expectedPrice = receiptLine.getUnitPrice();
        if (expectedPrice != null && expectedPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAllowedPrice = expectedPrice.multiply(new BigDecimal("1.05"));
            if (unitPrice.compareTo(maxAllowedPrice) > 0) {
                throw new ValidationException(String.format(
                        "3-way match failure: Billed unit price (%s) exceeds ordered price (%s) by more than 5%% tolerance for item [%s]",
                        unitPrice, expectedPrice, receiptLine.getItemCode()));
            }
        }
        com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory category = requested.taxCategory();
        if (category == null && receiptLine.getTaxCategory() != null) {
            category = receiptLine.getTaxCategory();
        }
        if (category == null && item != null && item.getTaxabilityType() != null) {
            category = switch (item.getTaxabilityType().toUpperCase()) {
                case "EXEMPT" -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.EXEMPT;
                case "ZERO_RATED" -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.ZERO_RATED;
                default -> com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE;
            };
        }
        if (category == null) {
            throw new com.erp.platform.common.exception.ValidationException(
                    "Tax category is required for purchase invoice line");
        }
        if (category == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.UNCLASSIFIED) {
            throw new com.erp.platform.common.exception.ValidationException(
                    "Tax category is required for purchase invoice line");
        }

        boolean isPanOnly = "PAN_ONLY".equalsIgnoreCase(invoice.getSupplierVatRegistrationStatus());
        if (isPanOnly) {
            if (requested.taxCategory() == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE
                    || (requested.taxRate() != null && requested.taxRate().compareTo(BigDecimal.ZERO) > 0)) {
                throw new ValidationException(
                        "Supplier " + invoice.getSupplierCode() + " is PAN-only (not registered for VAT). "
                                + "Under Nepal VAT Act 2052, VAT cannot be charged or claimed on invoices from PAN-only suppliers.");
            }
            category = com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.NON_TAXABLE;
        }

        BigDecimal taxRate = BigDecimal.ZERO;
        if (category == com.erp.business.procurement.purchaseinvoice.domain.PurchaseTaxCategory.TAXABLE) {
            com.erp.business.inventory.interfaces.tax.TaxRateService.TaxRate tr =
                    taxRateService.findActive(invoice.getTenantId(), invoice.getCompanyId(), "VAT", invoice.getInvoiceDate());
            if (tr == null) {
                throw new com.erp.platform.common.exception.ValidationException(
                        "No active VAT tax rate configured for company " + invoice.getCompanyId());
            }
            taxRate = tr.rate();
        }

        try {
            PurchaseInvoiceLine line = PurchaseInvoiceLine.bill(
                    lineNumber, receiptLine, quantity, unitPrice,
                    requested.discountPercentage(), BigDecimal.ZERO, requested.description());
            line.calculateTax(category, unitPrice, quantity, taxRate,
                    item != null && Boolean.TRUE.equals(item.getIsVatInclusive()),
                    requested.discountPercentage() != null ? requested.discountPercentage() : BigDecimal.ZERO);
            return line;
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                    "Line " + lineNumber + " of invoice " + invoice.getInvoiceNumber()
                            + " is not valid: " + ex.getMessage());
        }
    }

    /**
     * Finds a receipt line by id, refusing one that belongs to a different delivery.
     *
     * <p>Without the membership check a caller could name a receipt line from another
     * goods receipt - or another company's - and have it billed onto this invoice.
     */
    private GoodsReceiptLine findReceiptLine(GoodsReceipt receipt, Long receiptLineId) {
        return receipt.getLines().stream()
                .filter(line -> receiptLineId.equals(line.getId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Goods receipt line " + receiptLineId + " is not on goods receipt "
                                + receipt.getGrnNumber()));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_VIEW)
    public PurchaseInvoiceResponse getById(Long tenantId, Long companyId, Long id) {
        return toResponse(invoiceRepository
                .findByTenantIdAndCompanyIdAndId(tenantId, companyId, id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase invoice " + id + " was not found")));
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_VIEW)
    public Page<PurchaseInvoiceResponse> findAll(Long tenantId, Long companyId, Pageable pageable) {
        return invoiceRepository.findInScope(tenantId, companyId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_VIEW)
    public Page<PurchaseInvoiceResponse> findOutstanding(Long tenantId, Long companyId,
                                                          Pageable pageable) {
        return findOutstanding(tenantId, companyId, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_VIEW)
    public Page<PurchaseInvoiceResponse> findOutstanding(Long tenantId, Long companyId,
                                                          Long supplierId,
                                                          Pageable pageable) {
        if (supplierId != null) {
            return invoiceRepository.findOutstandingBySupplier(tenantId, companyId, supplierId, pageable)
                    .map(this::toResponse);
        }
        return invoiceRepository.findOutstanding(tenantId, companyId, pageable)
                .map(this::toResponse);
    }

    private PurchaseInvoiceResponse toResponse(PurchaseInvoice invoice) {
        List<PurchaseInvoiceResponse.Line> lines = new ArrayList<>();
        for (PurchaseInvoiceLine line : invoice.getLines()) {
            lines.add(new PurchaseInvoiceResponse.Line(
                    line.getId(),
                    line.getLineNumber(),
                    line.getGoodsReceiptLineId(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getUomCode(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getDiscountPercentage(),
                    line.getTaxRate(),
                    line.getLineAmount(),
                    line.taxAmount(),
                    line.getDescription()));
        }

        return new PurchaseInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getTenantId(),
                invoice.getCompanyId(),
                invoice.getBranchId(),
                invoice.getFiscalYearId(),
                invoice.getInvoiceType(),
                invoice.getPurchaseOrderId(),
                invoice.getPoNumber(),
                invoice.getGoodsReceiptId(),
                invoice.getGrnNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getSupplierId(),
                invoice.getSupplierCode(),
                invoice.getSupplierName(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getSubtotalAmount(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getIsTdsApplicable(),
                invoice.getTdsRate(),
                invoice.getTdsAmount(),
                invoice.getNetPayableAmount(),
                invoice.getPaidAmount(),
                invoice.getBalanceAmount(),
                invoice.getNotes(),
                invoice.getCancellationReason(),
                invoice.getCreatedAt(),
                lines);
    }
}
