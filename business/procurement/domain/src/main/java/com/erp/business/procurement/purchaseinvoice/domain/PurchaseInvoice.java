package com.erp.business.procurement.purchaseinvoice.domain;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
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
 * A supplier's invoice for goods the company has received: the payable.
 *
 * <p><strong>This is where the money is owed.</strong> The purchase order is a
 * commitment, the receipt is proof the goods arrived, and this document is the actual
 * claim. It is recorded APPROVED on creation because the supplier has already issued it
 * - see {@link PurchaseInvoiceStatus}.
 *
 * <p><strong>Balance is derived, never accepted.</strong> {@code paidAmount} is
 * incremented only by {@link #recordPayment}, and {@code balanceAmount} is recomputed
 * from it. There is no setter that could make the two disagree, which is what an
 * accounts-payable balance needs: the figure owed is whatever the total says minus
 * whatever has actually been paid.
 *
 * <p><strong>Overpayment is refused.</strong> {@link #recordPayment} will not let a
 * payment take the balance below zero, so the payable cannot quietly become a receivable.
 */
@Entity
@Table(
        name = "purchase_invoices",
        uniqueConstraints = {
                // Scoped by supplier as well as company: the number is the supplier's own,
                // and two suppliers both numbering from one is normal. Keying it on the
                // company alone would reject the second supplier's legitimate INV-0001.
                @UniqueConstraint(
                        name = "uk_purchase_invoices_supplier_number",
                        columnNames = {"tenant_id", "company_id", "supplier_id", "invoice_number"}
                ),
                // One invoice per goods receipt. Without this the same delivery could be
                // billed twice, doubling the payable for one set of goods.
                @UniqueConstraint(
                        name = "uk_purchase_invoices_receipt",
                        columnNames = {"tenant_id", "company_id", "goods_receipt_id"}
                )
        },
        indexes = {
                @Index(name = "idx_purchase_invoices_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_purchase_invoices_supplier",
                        columnList = "tenant_id, company_id, supplier_id, invoice_date"),
                @Index(name = "idx_purchase_invoices_order",
                        columnList = "tenant_id, company_id, purchase_order_id, invoice_date"),
                @Index(name = "idx_purchase_invoices_status",
                        columnList = "tenant_id, company_id, status, due_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseInvoice extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    @NotBlank(message = "Invoice number is required")
    @Column(name = "invoice_number", nullable = false, length = 60)
    private String invoiceNumber;

    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @Column(name = "branch_id")
    private Long branchId;

    /** The order whose goods are being billed. */
    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "po_number", nullable = false, length = 40)
    private String poNumber;

    /** The receipt that evidences the delivery. One invoice per receipt. */
    @Column(name = "goods_receipt_id", nullable = false)
    private Long goodsReceiptId;

    @Column(name = "grn_number", nullable = false, length = 40)
    private String grnNumber;

    @NotNull(message = "Invoice date is required")
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull(message = "Supplier is required")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseInvoiceStatus status = PurchaseInvoiceStatus.APPROVED;

    /** Net of the invoice lines. Derived, never accepted. */
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Cumulative settled amount. Written only by {@link #recordPayment}. */
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** What is still owed: total minus paid. Derived. */
    @Column(name = "balance_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "purchaseInvoice", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<PurchaseInvoiceLine> lines = new ArrayList<>();

    /**
     * Creates an approved invoice against a received delivery.
     *
     * <p>Supplier, currency, branch and fiscal year are copied from the receipt. An
     * invoice is a claim about a specific delivery, and the supplier on that delivery is
     * not a choice the accounts clerk makes.
     *
     * @param invoiceNumber the supplier's own invoice number
     * @param invoiceDate   the invoice date, defaults to today when null
     * @param dueDate       when payment falls due, may be null
     * @param receipt       the receipt being billed
     * @param notes         free text notes, may be null
     * @return a new unsaved invoice with no lines
     */
    public static PurchaseInvoice approved(String invoiceNumber, LocalDate invoiceDate,
                                           LocalDate dueDate, GoodsReceipt receipt, String notes) {

        return PurchaseInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate == null ? LocalDate.now() : invoiceDate)
                .dueDate(dueDate)
                .purchaseOrderId(receipt.getPurchaseOrderId())
                .poNumber(receipt.getPoNumber())
                .goodsReceiptId(receipt.getId())
                .grnNumber(receipt.getGrnNumber())
                .fiscalYearId(receipt.getFiscalYearId())
                .branchId(receipt.getBranchId())
                .supplierId(receipt.getSupplierId())
                .supplierCode(receipt.getSupplierCode())
                .supplierName(receipt.getSupplierName())
                .currency(receipt.getCurrency())
                .status(PurchaseInvoiceStatus.APPROVED)
                .notes(notes)
                .lines(new ArrayList<>())
                .build();
    }

    /**
     * Adds a line and refreshes the header totals.
     *
     * @param line the line to add
     */
    public void addLine(PurchaseInvoiceLine line) {
        if (lines.stream().anyMatch(existing ->
                existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this invoice");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    /**
     * Recomputes subtotal, tax and total from the lines, then re-derives the balance.
     *
     * <p>The payable figure accounts payable reports is {@link #balanceAmount}, and it
     * is only ever written here - never supplied by a caller.
     */
    public void recalculateTotals() {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurchaseInvoiceLine line : lines) {
            net = net.add(line.getLineAmount());
            tax = tax.add(line.taxAmount());
        }
        this.subtotalAmount = net.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = net.add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.balanceAmount = this.totalAmount.subtract(this.paidAmount)
                .max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Applies a payment to this invoice and re-derives its state.
     *
     * @param amount the amount paid, must be positive
     * @throws IllegalStateException if the invoice is cancelled or the amount would
     *         overpay it
     */
    public void recordPayment(BigDecimal amount) {
        if (status == PurchaseInvoiceStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot pay a cancelled invoice " + invoiceNumber);
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (totalAmount.signum() <= 0) {
            throw new IllegalStateException("Invoice " + invoiceNumber + " has no amount to pay");
        }

        BigDecimal newPaid = paidAmount.add(amount);
        if (newPaid.compareTo(totalAmount) > 0) {
            throw new IllegalStateException(
                    "Paying " + amount.toPlainString() + " would exceed the balance of "
                            + balanceAmount.toPlainString() + " on invoice " + invoiceNumber);
        }

        this.paidAmount = newPaid.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.balanceAmount = totalAmount.subtract(this.paidAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.status = PurchaseInvoiceStatus.fromPaidAmount(totalAmount, this.paidAmount);
    }

    /**
     * Voids this invoice. A reason is required.
     *
     * <p>Refused once payment has been made: a settled invoice cannot be voided, because
     * undoing it would need a reversing payment that this document no longer represents.
     *
     * @param actor  the acting user id
     * @param reason why the invoice is being voided
     */
    public void cancel(String actor, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to cancel an invoice");
        }
        if (status == PurchaseInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice " + invoiceNumber + " is already cancelled");
        }
        if (paidAmount.signum() > 0) {
            throw new IllegalStateException(
                    "Invoice " + invoiceNumber + " cannot be cancelled because "
                            + paidAmount.toPlainString() + " has already been paid against it");
        }
        this.status = PurchaseInvoiceStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelledBy = actor;
        this.cancellationReason = reason;
    }
}
