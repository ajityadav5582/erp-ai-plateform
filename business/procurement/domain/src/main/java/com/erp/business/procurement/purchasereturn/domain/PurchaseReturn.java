package com.erp.business.procurement.purchasereturn.domain;

import com.erp.business.procurement.goodsreceipt.domain.StockPostStatus;
import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Return / Debit Note aggregate root.
 *
 * <p>Represents physical goods shipped back to a supplier and the legal reversal of
 * accounts payable liability and Input VAT under Nepal VAT Act 2052 (Rule 23).
 */
@Entity
@Table(
        name = "purchase_returns",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_returns_tenant_company_number",
                columnNames = {"tenant_id", "company_id", "debit_note_number"}
        ),
        indexes = {
                @Index(name = "idx_purchase_returns_tenant_company", columnList = "tenant_id, company_id"),
                @Index(name = "idx_purchase_returns_invoice", columnList = "tenant_id, company_id, purchase_invoice_id, return_date"),
                @Index(name = "idx_purchase_returns_supplier", columnList = "tenant_id, company_id, supplier_id, return_date"),
                @Index(name = "idx_purchase_returns_status", columnList = "tenant_id, company_id, status, return_date"),
                @Index(name = "idx_purchase_returns_fiscal_year", columnList = "tenant_id, company_id, fiscal_year_id, return_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseReturn extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;
    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "Debit note number is required")
    @Column(name = "debit_note_number", nullable = false, length = 40)
    private String debitNoteNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    @NotNull(message = "Purchase invoice ID is required")
    @Column(name = "purchase_invoice_id", nullable = false)
    private Long purchaseInvoiceId;

    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;

    @Column(name = "goods_receipt_id")
    private Long goodsReceiptId;

    @Column(name = "grn_number", length = 40)
    private String grnNumber;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "po_number", length = 40)
    private String poNumber;

    @NotNull(message = "Supplier is required")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @NotBlank(message = "Supplier code is required")
    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @NotBlank(message = "Supplier name is required")
    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "supplier_pan_number", length = 64)
    private String supplierPanNumber;

    @NotNull(message = "Warehouse ID is required")
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    @NotNull(message = "Return date is required")
    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "return_date_np", length = 20)
    private String returnDateNp;

    @NotBlank(message = "Return reason is legally required")
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseReturnStatus status = PurchaseReturnStatus.DRAFT;

    @NotNull(message = "Stock post status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_post_status", nullable = false, length = 20)
    @Builder.Default
    private StockPostStatus stockPostStatus = StockPostStatus.PENDING;

    @Column(name = "stock_post_error", length = 500)
    private String stockPostError;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<PurchaseReturnLine> lines = new ArrayList<>();

    public static PurchaseReturn draft(
            String debitNoteNumber,
            PurchaseInvoice invoice,
            Long warehouseId,
            String warehouseCode,
            String warehouseName,
            LocalDate returnDate,
            String returnDateNp,
            String reason,
            String notes
    ) {
        if (invoice == null) {
            throw new IllegalArgumentException("Purchase return must reference an original invoice");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Return reason is required under Nepal VAT Rule 23");
        }

        return PurchaseReturn.builder()
                .debitNoteNumber(debitNoteNumber)
                .fiscalYearId(invoice.getFiscalYearId())
                .branchId(invoice.getBranchId())
                .purchaseInvoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .goodsReceiptId(invoice.getGoodsReceiptId())
                .grnNumber(invoice.getGrnNumber())
                .purchaseOrderId(invoice.getPurchaseOrderId())
                .poNumber(invoice.getPoNumber())
                .supplierId(invoice.getSupplierId())
                .supplierCode(invoice.getSupplierCode())
                .supplierName(invoice.getSupplierName())
                .supplierPanNumber(invoice.getSupplierTaxId() != null ? invoice.getSupplierTaxId() : invoice.getSupplierVatRegistrationNumber())
                .warehouseId(warehouseId)
                .warehouseCode(warehouseCode)
                .warehouseName(warehouseName)
                .returnDate(returnDate != null ? returnDate : LocalDate.now())
                .returnDateNp(returnDateNp)
                .reason(reason.trim())
                .notes(notes)
                .currency(invoice.getCurrency() != null ? invoice.getCurrency() : "NPR")
                .status(PurchaseReturnStatus.DRAFT)
                .stockPostStatus(StockPostStatus.PENDING)
                .build();
    }

    public void addLine(PurchaseReturnLine line) {
        if (this.status != PurchaseReturnStatus.DRAFT) {
            throw new IllegalStateException("Lines can only be added to a DRAFT return");
        }
        line.assignToReturn(this);
        this.lines.add(line);
        recalculateTotals();
    }

    public void clearLines() {
        if (this.status != PurchaseReturnStatus.DRAFT) {
            throw new IllegalStateException("Lines can only be cleared on a DRAFT return");
        }
        this.lines.clear();
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurchaseReturnLine line : lines) {
            subtotal = subtotal.add(line.getSubtotalAmount() != null ? line.getSubtotalAmount() : BigDecimal.ZERO);
            tax = tax.add(line.getTaxAmount() != null ? line.getTaxAmount() : BigDecimal.ZERO);
        }
        this.subtotalAmount = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = this.subtotalAmount.add(this.taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public void approve(String actor) {
        if (this.status != PurchaseReturnStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase returns can be approved");
        }
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("Cannot approve a purchase return without line items");
        }
        this.status = PurchaseReturnStatus.APPROVED;
        this.approvedAt = Instant.now();
        this.approvedBy = actor;
    }

    public void post(String actor) {
        if (this.status != PurchaseReturnStatus.APPROVED && this.status != PurchaseReturnStatus.DRAFT) {
            throw new IllegalStateException("Purchase return must be DRAFT or APPROVED before posting");
        }
        if (this.lines.isEmpty()) {
            throw new IllegalStateException("Cannot post a purchase return without line items");
        }
        this.status = PurchaseReturnStatus.POSTED;
        this.postedAt = Instant.now();
        this.postedBy = actor;
    }

    public void markStockPosted() {
        this.stockPostStatus = StockPostStatus.POSTED;
        this.stockPostError = null;
    }

    public void markStockFailed(String error) {
        this.stockPostStatus = StockPostStatus.FAILED;
        this.stockPostError = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }

    public void cancel(String actor, String reason) {
        if (this.status == PurchaseReturnStatus.CANCELLED) {
            throw new IllegalStateException("Purchase return " + debitNoteNumber + " is already cancelled");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A cancellation reason is required");
        }
        this.status = PurchaseReturnStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason.trim();
    }
}

