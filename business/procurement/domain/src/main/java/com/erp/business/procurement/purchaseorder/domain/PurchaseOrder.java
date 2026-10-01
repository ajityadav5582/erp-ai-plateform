package com.erp.business.procurement.purchaseorder.domain;

import com.erp.business.procurement.shared.domain.TenantScopedEntity;
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
import java.util.Objects;

/**
 * Purchase order aggregate root: the commercial commitment to a supplier.
 *
 * <p><strong>Aggregate boundary.</strong> A purchase order owns its lines and
 * nothing else. Supplier and item records live in the inventory service and are
 * referenced by id with a frozen snapshot of their display fields. The header
 * owns the status machine, the totals and the invariant that the sum of the
 * lines is the only source of the document totals.
 *
 * <p><strong>Totals are derived, never accepted.</strong> There is no
 * {@code setTotal(...)}. {@link #recalculateTotals()} is the single writer,
 * called after every line mutation, so the header total cannot disagree with
 * the lines that produce it. The values are still persisted, because list
 * screens and reports must not have to aggregate the line table on every read.
 *
 * <p><strong>Concurrency.</strong> The inherited {@code version} column gives
 * optimistic locking: two buyers editing the same PO produce an
 * {@code OptimisticLockingFailureException} on the loser rather than a silent
 * lost update.
 */
@Entity
@Table(
        name = "purchase_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_orders_tenant_company_number",
                columnNames = {"tenant_id", "company_id", "po_number"}
        ),
        indexes = {
                @Index(name = "idx_purchase_orders_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_purchase_orders_supplier",
                        columnList = "tenant_id, company_id, supplier_id, order_date"),
                @Index(name = "idx_purchase_orders_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, order_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseOrder extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * Human readable document number, allocated by the
     * {@code next_document_number()} database function. Unique per tenant and
     * company, which is the only scope in which numbers are meaningful.
     */
    @NotBlank(message = "PO number is required")
    @Column(name = "po_number", nullable = false, length = 40)
    private String poNumber;

    /** Fiscal year the order belongs to, for period reporting. May be null pre-assignment. */
    @Column(name = "fiscal_year_id")
    private Long fiscalYearId;

    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /**
     * Supplier reference into the inventory service. No database foreign key:
     * a cross-service constraint would couple the two services' migration
     * timelines. The service validates the supplier exists within the same
     * tenant and company before persisting.
     */
    @NotNull(message = "Supplier is required")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    /** Frozen supplier snapshot: the name printed on the issued document. */
    @NotBlank(message = "Supplier code is required")
    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @NotBlank(message = "Supplier name is required")
    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    /**
     * A purchase order is single currency. Lines carry no currency of their
     * own, which removes the possibility of a mixed-currency document.
     */
    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "NPR";

    // ---- Derived totals. Written only by recalculateTotals(). ----
    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "shipping_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "received_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal receivedAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays;

    @Column(name = "delivery_location", length = 255)
    private String deliveryLocation;

    @Column(name = "buyer_id")
    private Long buyerId;

    /** Free reference such as the requisition or quotation this order arose from. */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- Workflow stamps: NULL until the transition happens. ----
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * The lines of this order.
     *
     * <p>{@code cascade = ALL} because lines have no meaning outside their
     * header, and {@code orphanRemoval = true} so that removing the last
     * reference from a draft deletes the row rather than orphaning it.
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    @ToString.Exclude
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    /**
     * Creates a draft purchase order.
     *
     * <p>{@code poNumber} must be supplied by the caller, which obtains it from
     * the {@code next_document_number()} function inside the same transaction as
     * the insert. Generating it in Java would reintroduce the duplicate-number
     * race that the sequence table exists to remove.
     *
     * @param poNumber       the allocated document number
     * @param orderDate      the order date, must not be null
     * @param supplierId     the supplier id
     * @param supplierCode   the supplier code snapshot
     * @param supplierName   the supplier name snapshot
     * @param currency       the ISO 4217 currency code
     * @param fiscalYearId   the owning fiscal year, may be null
     * @return a new unsaved draft order with no lines
     */
    public static PurchaseOrder draft(String poNumber, LocalDate orderDate, Long supplierId,
                                      String supplierCode, String supplierName, String currency,
                                      Long fiscalYearId) {
        Objects.requireNonNull(orderDate, "Order date is required");
        Objects.requireNonNull(supplierId, "Supplier is required");

        PurchaseOrder order = PurchaseOrder.builder()
                .poNumber(poNumber)
                .orderDate(orderDate)
                .supplierId(supplierId)
                .supplierCode(supplierCode)
                .supplierName(supplierName)
                .currency(normalizeCurrency(currency))
                .fiscalYearId(fiscalYearId)
                .status(PurchaseOrderStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();
        order.recalculateTotals();
        return order;
    }

    /**
     * Adds a line to a draft order and refreshes the header totals.
     *
     * @param line the line to add, must already be constructed via
     *             {@link PurchaseOrderLine#create}
     * @throws IllegalStateException if the order is not editable, or the line number is taken
     */
    public void addLine(PurchaseOrderLine line) {
        requireEditable();
        Objects.requireNonNull(line, "Line is required");
        if (lines.stream().anyMatch(existing -> existing.getLineNumber().equals(line.getLineNumber()))) {
            throw new IllegalStateException(
                    "Line number " + line.getLineNumber() + " is already used on this order");
        }
        line.assignScope(getTenantId(), getCompanyId());
        line.attachTo(this);
        lines.add(line);
        recalculateTotals();
    }

    /**
     * Replaces the whole line set of a draft order in one step.
     *
     * <p>Preferred over add-then-remove when the client edits a full grid: the
     * collection is swapped once and the totals are recalculated once, so no
     * intermediate flush can persist a half-applied edit.
     *
     * @param newLines the replacement lines, must not be null
     * @throws IllegalStateException if the order is not editable, or a line number is duplicated
     */
    public void replaceLines(List<PurchaseOrderLine> newLines) {
        requireEditable();
        Objects.requireNonNull(newLines, "Lines are required");

        long distinct = newLines.stream()
                .map(PurchaseOrderLine::getLineNumber)
                .distinct()
                .count();
        if (distinct != newLines.size()) {
            throw new IllegalStateException("Duplicate line numbers in the submitted line set");
        }
        newLines.forEach(line -> {
            line.assignScope(getTenantId(), getCompanyId());
            line.attachTo(this);
        });
        this.lines.clear();
        this.lines.addAll(newLines);
        recalculateTotals();
    }

    /**
     * Sets the freight charge and refreshes the totals.
     *
     * @param shippingAmount the freight amount, must not be negative
     */
    public void setShippingAmount(BigDecimal shippingAmount) {
        if (shippingAmount == null || shippingAmount.signum() < 0) {
            throw new IllegalArgumentException("Shipping amount cannot be negative");
        }
        this.shippingAmount = shippingAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        recalculateTotals();
    }

    /**
     * Records a goods receipt against a line and moves the header status to
     * match.
     *
     * @param lineNumber       the 1-based line ordinal
     * @param receivedQuantity the quantity received on this call
     * @return the resulting header status
     * @throws IllegalStateException if the order is not in a receivable state
     */
    public PurchaseOrderStatus receiveAgainstLine(int lineNumber, BigDecimal receivedQuantity) {
        if (status != PurchaseOrderStatus.APPROVED
                && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException(
                    "Goods cannot be received against a purchase order in state " + status);
        }
        PurchaseOrderLine line = findLine(lineNumber);
        PurchaseOrderLineStatus lineStatus = line.receive(receivedQuantity);
        recalculateTotals();
        this.status = deriveReceiptStatus(lineStatus);
        return this.status;
    }

    /**
     * Moves the document to a new lifecycle state, stamping the matching
     * workflow columns.
     *
     * <p>Rejecting or cancelling requires a reason: an unexplained rejection is
     * not auditable.
     *
     * @param target the requested next state
     * @param actor  the acting user id
     * @param reason the rejection or cancellation reason, may be null otherwise
     * @throws IllegalStateException if the transition is not permitted
     */
    public void transitionTo(PurchaseOrderStatus target, String actor, String reason) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot move a purchase order from " + status + " to " + target);
        }
        Instant now = Instant.now();
        switch (target) {
            case SUBMITTED -> {
                this.submittedAt = now;
                this.submittedBy = actor;
            }
            case APPROVED -> {
                this.approvedAt = now;
                this.approvedBy = actor;
            }
            case REJECTED -> {
                requireReason(reason, "rejection");
                this.rejectedAt = now;
                this.rejectedBy = actor;
                this.rejectionReason = reason;
            }
            case CANCELLED -> {
                requireReason(reason, "cancellation");
                this.cancelledAt = now;
                this.cancelledBy = actor;
                this.cancellationReason = reason;
            }
            case CLOSED -> this.closedAt = now;
            default -> {
                // PARTIALLY_RECEIVED and RECEIVED are set by receiveAgainstLine only.
            }
        }
        this.status = target;
    }

    /**
     * Recomputes every header total from the current line set.
     *
     * <p>Called after each mutation rather than lazily on read so the persisted
     * values are always current, which is what lets list queries and reports
     * read {@code totalAmount} directly instead of aggregating lines.
     *
     * <p>Cancelled lines are excluded, which is what makes line cancellation a
     * safe way to amend a draft without recomputing anything by hand.
     */
    public void recalculateTotals() {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;

        for (PurchaseOrderLine line : lines) {
            if (!line.isCounted()) {
                continue;
            }
            BigDecimal lineGross = line.getQuantity().multiply(line.getUnitPrice());
            gross = gross.add(lineGross);
            discount = discount.add(
                    lineGross.multiply(line.getDiscountPercentage())
                            .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP));
            tax = tax.add(line.taxAmount());
            BigDecimal receivedRatio = line.getReceivedQuantity()
                    .divide(line.getQuantity(), 10, RoundingMode.HALF_UP);
            received = received.add(line.netAmount().multiply(receivedRatio)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        this.subtotalAmount = gross.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.discountAmount = discount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.receivedAmount = received.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal total = gross.subtract(discount).add(tax).add(this.shippingAmount);
        this.totalAmount = total.max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the outstanding goods value, the figure accounts payable needs.
     *
     * @return total minus received, never negative
     */
    public BigDecimal outstandingAmount() {
        return totalAmount.subtract(receivedAmount).max(BigDecimal.ZERO);
    }

    /**
     * Returns the line with the given ordinal.
     *
     * @param lineNumber the 1-based ordinal
     * @return the matching line
     * @throws IllegalArgumentException if no such line exists
     */
    public PurchaseOrderLine findLine(int lineNumber) {
        return lines.stream()
                .filter(line -> line.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase order has no line " + lineNumber));
    }

    /**
     * Returns the lines that still have outstanding quantity.
     *
     * @return the outstanding lines, in line number order
     */
    public List<PurchaseOrderLine> outstandingLines() {
        return lines.stream().filter(PurchaseOrderLine::hasOutstandingQuantity).toList();
    }

    /**
     * Returns whether this order may still have its content edited.
     *
     * @return {@code true} while the order is a draft
     */
    public boolean isEditable() {
        return status.isEditable();
    }

    private PurchaseOrderStatus deriveReceiptStatus(PurchaseOrderLineStatus lastLineStatus) {
        if (lastLineStatus == PurchaseOrderLineStatus.RECEIVED
                || outstandingLines().isEmpty()) {
            return PurchaseOrderStatus.RECEIVED;
        }
        return PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    private void requireEditable() {
        if (!isEditable()) {
            throw new IllegalStateException(
                    "Purchase order " + poNumber + " is " + status
                            + "; its lines can only be changed while it is DRAFT");
        }
    }

    private static void requireReason(String reason, String label) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to record a " + label);
        }
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "NPR";
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3 letter ISO 4217 code");
        }
        return normalized;
    }
}
