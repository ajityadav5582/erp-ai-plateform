package com.erp.business.procurement.purchaseorder.domain;

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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_purchase_orders_tenant_company_number",
                        columnNames = {"tenant_id", "company_id", "po_number"}
                ),
                // One requisition converts to at most one order through this route.
                // NULLs are exempt, so orders raised without a requisition are unaffected.
                @UniqueConstraint(
                        name = "uk_purchase_orders_source_requisition",
                        columnNames = {"tenant_id", "company_id", "source_purchase_requisition_id"}
                )
        },
        indexes = {
                @Index(name = "idx_purchase_orders_tenant_company",
                        columnList = "tenant_id, company_id"),
                @Index(name = "idx_purchase_orders_supplier",
                        columnList = "tenant_id, company_id, supplier_id, order_date"),
                @Index(name = "idx_purchase_orders_fiscal_year",
                        columnList = "tenant_id, company_id, fiscal_year_id, order_date"),
                @Index(name = "idx_purchase_orders_source_requisition",
                        columnList = "tenant_id, company_id, source_purchase_requisition_id")
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

    /**
     * The requisition this order was converted from.
     *
     * <p>Null for an order raised directly. Part of a unique key with tenant and
     * company, which is what makes a second conversion of the same requisition
     * impossible even if two requests race past the application-level check.
     */
    @Column(name = "source_purchase_requisition_id")
    private Long sourcePurchaseRequisitionId;

    /** Owning branch, copied from the source requisition on conversion. */
    @Column(name = "branch_id")
    private Long branchId;

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

    /**
     * The nominated destination for this order's goods.
     *
     * <p>A nomination, not a commitment. It is what the supplier is instructed to
     * deliver to, and goods get redirected constantly in multi-site businesses -
     * so a goods receipt naming a different warehouse is a routine occurrence, not
     * an error. The receipt records where the goods actually landed; this records
     * where they were meant to go, and comparing the two is what surfaces the
     * difference.
     *
     * <p>Nullable, because a requisition may name no destination and the buyer may
     * not settle one before issuing the order. Resolution falls through to the
     * company default warehouse at receipt time.
     *
     * <p>A bare id with no cross-service foreign key, in the same shape as
     * {@code supplierId}; the warehouse is mastered in the inventory service.
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** Frozen warehouse code and name, so a rename never rewrites a sent order. */
    @Column(name = "warehouse_code", length = 64)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

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
    @Builder.Default
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
        return build(poNumber, orderDate, supplierId, supplierCode, supplierName,
                currency, fiscalYearId, null, null, null, null);
    }

    /**
     * Creates a draft order raised from an approved requisition.
     *
     * <p>The source id is what makes the conversion traceable and what the unique
     * key uses to refuse a duplicate.
     */
    public static PurchaseOrder fromRequisition(String poNumber, LocalDate orderDate,
                                                Long supplierId, String supplierCode,
                                                String supplierName, String currency,
                                                Long fiscalYearId, Long branchId,
                                                Long sourceRequisitionId) {
        return build(poNumber, orderDate, supplierId, supplierCode, supplierName,
                currency, fiscalYearId, branchId, sourceRequisitionId,
                null, null);
    }

    private static PurchaseOrder build(String poNumber, LocalDate orderDate, Long supplierId,
                                       String supplierCode, String supplierName, String currency,
                                       Long fiscalYearId, Long branchId, Long sourceRequisitionId,
                                       String reference, String notes) {
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
                .branchId(branchId)
                .sourcePurchaseRequisitionId(sourceRequisitionId)
                .reference(reference)
                .notes(notes)
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
     * Applies the delivery destination and reference fields captured at conversion time.
     *
     * <p>Separate from the constructor because the requisition's nominated warehouse
     * and the buyer's expected date are known only once the conversion request has
     * been read, whereas the header itself is built before the lines.
     *
     * <p>The warehouse is only overwritten when the conversion actually names one.
     * Treating null as "clear" would let a requisition with no destination produce
     * an order that has silently lost one, so absence keeps whatever was already
     * there and the resolution falls through to the default at receipt.
     *
     * @param expectedDelivery  the committed delivery date, may be null
     * @param warehouseId       the nominated destination warehouse, or null to leave as is
     * @param warehouseCode     the nominated warehouse's frozen code
     * @param warehouseName     the nominated warehouse's frozen name
     * @param sourceReference   the requisition number this order came from
     * @param notes             free text notes, may be null
     */
    public void adoptDelivery(LocalDate expectedDelivery, Long warehouseId,
                              String warehouseCode, String warehouseName,
                              String sourceReference, String notes) {
        this.expectedDeliveryDate = expectedDelivery;
        if (warehouseId != null) {
            this.warehouseId = warehouseId;
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
        }
        this.reference = sourceReference;
        this.notes = notes;
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
     * Records a goods receipt against a line and re-derives the header status from
     * the whole line set.
     *
     * <p><strong>The header follows the lines, never the other way round.</strong>
     * The status is recomputed by {@link #deriveReceiptStatus()} from every line
     * after the receipt, because a receipt only ever touches one line at a time.
     * Deriving it from the line that happened to be received in this call was the
     * defect behind "Goods cannot be received against a purchase order in state
     * RECEIVED": receiving line 1 of a three line order in full promoted the whole
     * order to RECEIVED, and the two lines still outstanding could then never be
     * received because this method refuses a RECEIVED order. The order must stay
     * receivable for as long as any line is.
     *
     * @param lineNumber       the 1-based line ordinal
     * @param receivedQuantity the quantity received on this call, may be less than
     *                         the outstanding quantity for a short delivery
     * @return the resulting header status
     * @throws IllegalArgumentException if there is no such line
     * @throws IllegalStateException    if the order is not in a receivable state
     */
    public PurchaseOrderStatus receiveAgainstLine(int lineNumber, BigDecimal receivedQuantity) {
        requireReceivable();
        PurchaseOrderLine line = findLine(lineNumber);
        line.receive(receivedQuantity);
        applyDerivedReceiptStatus();
        return this.status;
    }

    /**
     * Unwinds a voided goods receipt against a line identified by its id.
     *
     * <p>The shape a voided receipt needs: a goods receipt note stores the
     * {@code purchase_order_line_id} it consumed, not the ordinal, so reversing it
     * means looking lines up by id.
     *
     * @param lineId           the order line id
     * @param reversedQuantity the quantity taken back off this line
     * @return the resulting header status
     * @throws IllegalArgumentException if no such line exists
     * @throws IllegalStateException    if the order is closed, cancelled or rejected
     */
    public PurchaseOrderStatus reverseReceiptAgainstLineId(Long lineId, BigDecimal reversedQuantity) {
        if (lineId == null) {
            throw new IllegalArgumentException("A line id is required");
        }
        PurchaseOrderLine line = findLineById(lineId);
        return reverseReceiptAgainstLine(line.getLineNumber(), reversedQuantity);
    }

    /**
     * Returns the line with the given id.
     *
     * @param lineId the line id
     * @return the matching line
     * @throws IllegalArgumentException if no such line exists
     */
    public PurchaseOrderLine findLineById(Long lineId) {
        return lines.stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Purchase order " + poNumber + " has no line with id " + lineId));
    }

    /**
     * Unwinds a voided goods receipt against a line and re-derives the header
     * status.
     *
     * <p>The counterpart of {@link #receiveAgainstLine(int, BigDecimal)}. This is
     * what lets a receipt posted against the wrong order, or counted twice, be
     * corrected: without it the consumed quantity is never returned and the order
     * is unrecoverable.
     *
     * <p>Terminal states are refused because they are the accounting record: a
     * closed order has been matched and settled, and an order that was cancelled
     * will never be delivered. Both need a new document, not a reversal.
     *
     * @param lineNumber       the 1-based line ordinal
     * @param reversedQuantity the quantity taken back off this line
     * @return the resulting header status
     * @throws IllegalStateException if the order is closed, cancelled or rejected
     */
    public PurchaseOrderStatus reverseReceiptAgainstLine(int lineNumber, BigDecimal reversedQuantity) {
        if (status.isTerminal()) {
            throw new IllegalStateException(
                    "Purchase order " + poNumber + " is " + status
                            + "; a voided receipt cannot be reversed against it");
        }
        PurchaseOrderLine line = findLine(lineNumber);
        line.reverseReceipt(reversedQuantity);
        applyDerivedReceiptStatus();
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
        // Checked before the state table, not after. From PARTIALLY_RECEIVED the
        // table refuses CLOSED outright, so a later check would never be reached and
        // the caller would be told only that the move is not allowed - with no
        // indication that the reason is goods still being on their way.
        if (target == PurchaseOrderStatus.CLOSED) {
            requireFullyReceivedForClose();
        }
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
     * Returns the tax-inclusive value still owed against this order.
     *
     * <p><strong>Measured on the same basis as {@link #totalAmount}.</strong> It was
     * {@code totalAmount - receivedAmount}, but those two are not on the same basis:
     * the total is net plus tax plus freight, whereas {@code receivedAmount} is the
     * net goods value only. On a fully received 20 unit order at 100.00 with 13%
     * tax that arithmetic reports 260.00 still owed - an order that has received
     * everything it ordered, still claiming money is outstanding against it.
     *
     * <p>So the shortfall is computed straight from the lines, on the total's own
     * basis: each line's net plus tax, less the proportion already received, plus
     * the freight while anything at all is still outstanding. Freight clears only
     * once the last item lands, because a part-delivered order has not been
     * freighted in full.
     *
     * @return the value still owing, never negative
     */
    public BigDecimal outstandingAmount() {
        BigDecimal orderedGross = BigDecimal.ZERO;
        BigDecimal receivedGross = BigDecimal.ZERO;

        for (PurchaseOrderLine line : lines) {
            if (!line.isCounted()) {
                continue;
            }
            BigDecimal lineGross = line.netAmount().add(line.taxAmount());
            orderedGross = orderedGross.add(lineGross);
            BigDecimal receivedRatio = line.getReceivedQuantity()
                    .divide(line.getQuantity(), 10, RoundingMode.HALF_UP);
            receivedGross = receivedGross.add(
                    lineGross.multiply(receivedRatio).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        BigDecimal outstanding = orderedGross.subtract(receivedGross);
        if (outstanding.signum() > 0) {
            outstanding = outstanding.add(this.shippingAmount);
        }
        return outstanding.max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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

    /**
     * Recomputes the header totals and the receipt status together.
     *
     * <p>Both are derived, so they are always written from the same line set in
     * the same step; a caller cannot end up with a fresh status over a stale total.
     */
    private void applyDerivedReceiptStatus() {
        recalculateTotals();
        this.status = deriveReceiptStatus();
    }

    /**
     * Derives the receipt status from <em>every</em> line of the order.
     *
     * <p>This is the single source of truth for whether the order is fully
     * received, and it is deliberately a function of the whole line set rather
     * than of the line touched by the last receipt:
     *
     * <ul>
     *   <li>Nothing counted left to deliver, and something was received →
     *       {@link PurchaseOrderStatus#RECEIVED}.</li>
     *   <li>Something still outstanding and something has arrived →
     *       {@link PurchaseOrderStatus#PARTIALLY_RECEIVED}, whether the shortfall
     *       is a whole undelivered line or a short quantity on one line.</li>
     *   <li>Nothing has arrived at all → stay {@link PurchaseOrderStatus#APPROVED}.</li>
     * </ul>
     *
     * <p>Cancelled lines are excluded by {@link #outstandingLines()}, so an order
     * whose remaining lines were all withdrawn closes out as received rather than
     * being permanently stuck.
     */
    private PurchaseOrderStatus deriveReceiptStatus() {
        boolean anyReceived = lines.stream()
                .filter(PurchaseOrderLine::isCounted)
                .anyMatch(line -> line.getReceivedQuantity().signum() > 0);

        if (outstandingLines().isEmpty()) {
            return anyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.APPROVED;
        }
        return anyReceived ? PurchaseOrderStatus.PARTIALLY_RECEIVED : PurchaseOrderStatus.APPROVED;
    }

    /**
     * Returns whether this order may still take goods in.
     *
     * <p>Delegates to {@link PurchaseOrderStatus#isReceivable()} rather than
     * repeating the state list, so the guard on the receipt path and the state
     * table cannot drift apart.
     *
     * @return {@code true} while issued to the supplier and not yet fully received
     */
    public boolean isReceivable() {
        return status.isReceivable();
    }

    /**
     * Guards the receipt path with a message that says what to do instead.
     *
     * <p>A closed, cancelled or rejected order is refused, but so is one that is
     * already fully received, and the two need different answers: the first is a
     * workflow problem, the second simply means there is nothing left to book.
     */
    private void requireReceivable() {
        if (isReceivable()) {
            return;
        }
        if (status == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException(
                    "Every line on purchase order " + poNumber
                            + " has already been received in full; there is nothing left to receive");
        }
        throw new IllegalStateException(
                "Goods cannot be received against a purchase order in state " + status);
    }

    /**
     * Refuses to close an order that still owes goods.
     *
     * <p>Closing means "matched and settled". Permitting it on a half delivered
     * order would retire the document while goods were still arriving, and every
     * later receipt would then be refused by {@link #requireReceivable()} - the
     * deliveries would exist physically and nowhere in the system.
     */
    private void requireFullyReceivedForClose() {
        if (!outstandingLines().isEmpty()) {
            throw new IllegalStateException(
                    "Purchase order " + poNumber + " still has "
                            + outstandingLines().size() + " line(s) awaiting delivery and cannot be closed");
        }
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
