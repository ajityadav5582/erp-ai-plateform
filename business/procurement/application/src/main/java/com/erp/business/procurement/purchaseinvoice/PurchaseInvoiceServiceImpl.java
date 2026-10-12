package com.erp.business.procurement.purchaseinvoice;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptLine;
import com.erp.business.procurement.goodsreceipt.domain.GoodsReceiptStatus;
import com.erp.business.procurement.goodsreceipt.infrastructure.persistence.GoodsReceiptRepository;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceLine;
import com.erp.business.procurement.purchaseinvoice.dto.CreateInvoiceRequest;
import com.erp.business.procurement.purchaseinvoice.dto.PurchaseInvoiceResponse;
import com.erp.business.procurement.purchaseinvoice.infrastructure.persistence.PurchaseInvoiceRepository;
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

    @Override
    @Transactional
    @RequiresPermission(ProcurementPermissions.PURCHASE_INVOICE_CREATE)
    public PurchaseInvoiceResponse create(Long tenantId, Long companyId,
                                          CreateInvoiceRequest request) {

        if (tenantId == null || companyId == null) {
            throw new ValidationException("A tenant and company scope are required");
        }

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

        BigDecimal quantity = requested.quantity() != null
                ? requested.quantity() : receiptLine.getReceivedQuantity();
        BigDecimal unitPrice = requested.unitPrice() != null
                ? requested.unitPrice() : receiptLine.getUnitPrice();
        BigDecimal taxRate = requested.taxRate() != null
                ? requested.taxRate() : BigDecimal.ZERO;

        try {
            return PurchaseInvoiceLine.bill(
                    lineNumber, receiptLine, quantity, unitPrice,
                    requested.discountPercentage(), taxRate, requested.description());
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
                invoice.getPaidAmount(),
                invoice.getBalanceAmount(),
                invoice.getNotes(),
                invoice.getCancellationReason(),
                invoice.getCreatedAt(),
                lines);
    }
}
