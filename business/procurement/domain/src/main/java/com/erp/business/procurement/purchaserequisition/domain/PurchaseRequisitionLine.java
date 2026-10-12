package com.erp.business.procurement.purchaserequisition.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Objects;

/**
 * One line of a branch purchase requisition: what is being asked for, and how
 * much of it has been granted and turned into an order.
 *
 * <p><strong>Why the item attributes are duplicated here.</strong> The line
 * stores {@code itemCode}, {@code itemName} and {@code uomCode} next to the
 * {@code itemId} reference. A requisition is a statement about what a branch
 * needed at a point in time. If those values were resolved live from the
 * catalogue, renaming an item or changing its base unit would silently rewrite
 * the meaning of approved history and of every budget report derived from it.
 *
 * <p><strong>Three quantities, not one.</strong>
 * {@code requestedQuantity} is what the branch asked for,
 * {@code approvedQuantity} is what it was granted, and
 * {@code orderedQuantity} is what procurement has actually committed to buy.
 * Collapsing these into a single quantity is the most common way requisition
 * modules go wrong: it makes partial approval invisible and makes it impossible
 * to answer "how much of what was approved is still un-ordered?".
 *
 * <p><strong>Why amounts are computed in Java here.</strong> The header
 * recalculates on every mutation and must see a value immediately, before the
 * entity is flushed, so a pure function over the stored inputs is used rather
 * than a database generated column that would only be readable after a
 * round-trip. The arithmetic is exposed twice over the same private helpers -
 * once keyed on {@code requestedQuantity} and once on {@code approvedQuantity}
 * - so there is exactly one implementation of the multiplication and one of
 * the tax rounding, and no caller can drift from the other.
 *
 * <p>Lines are mutable only while the parent is editable. {@link
 * PurchaseRequisition} owns that rule so it cannot be bypassed by assigning the
 * line directly.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisition_lines",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_purchase_requisition_lines_requisition_number",
                        columnNames = {"purchase_requisition_id", "line_number"}
                ),
                @UniqueConstraint(
                        name = "uk_purchase_requisition_lines_id_scope",
                        columnNames = {"id", "tenant_id", "company_id"}
                )
        },
        indexes = {
                @Index(name = "idx_purchase_requisition_lines_requisition",
                        columnList = "purchase_requisition_id, line_number"),
                @Index(name = "idx_purchase_requisition_lines_item_consolidation",
                        columnList = "tenant_id, company_id, item_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisitionLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** Stable ordinal within the document, 1-based. Never re-used after submission. */
    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** Reference into the inventory catalogue. Not a database foreign key: cross-service. */
    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Frozen snapshot of the item at request time. */
    @NotBlank(message = "Item code is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "Unit of measure is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    /** Reference to the Unit master data. Not a database foreign key: cross-service. */
    @Column(name = "unit_id")
    private Long unitId;

    /**
     * What the branch asked for. Must be positive.
     *
     * <p>The header's {@code estimated_*} totals and the amount the approval
     * policy is resolved against are both derived from this quantity, so it is
     * the figure that must be right from the moment the line is created.
     */
    @NotNull(message = "Requested quantity is required")
    @Column(name = "requested_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal requestedQuantity;

    /**
     * What was granted. Never negative, and never above
     * {@link #requestedQuantity} without the document being resubmitted.
     */
    @NotNull(message = "Approved quantity is required")
    @Column(name = "approved_quantity", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal approvedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);

    /**
     * What procurement has committed to buy against this line. Never negative
     * and never above {@link #approvedQuantity}; over-ordering is rejected here
     * as well as by a database constraint.
     */
    @NotNull(message = "Ordered quantity is required")
    @Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal orderedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);

    /**
     * Planning price per unit.
     *
     * <p>This is an <em>estimate</em>, not a contracted price. The agreed price
     * is set on the purchase order after RFQ. Storing the estimate here is what
     * lets a branch test affordability before the spend is committed.
     */
    @NotNull(message = "Estimated unit price is required")
    @Column(name = "estimated_unit_price", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal estimatedUnitPrice = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Tax percentage, 0 to 100. */
    @NotNull(message = "Tax rate is required")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO.setScale(2);

    @NotNull(message = "Line status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PurchaseRequisitionLineStatus status = PurchaseRequisitionLineStatus.REQUESTED;

    /** Why this line is needed, when the item itself needs no explanation. */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /**
     * Owning requisition. Set by {@link PurchaseRequisition#addLine}; a line is
     * never attached by the caller directly.
     *
     * <p>Mapped without a cascade and without {@code insertable}: the header
     * owns the lifecycle, so saving the header persists the lines.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseRequisition requisition;

    /**
     * Links this line to its parent requisition. Package-private on purpose:
     * the aggregate root is the only writer of this field.
     *
     * @param owningRequisition the owning requisition
     */
    void attachTo(PurchaseRequisition owningRequisition) {
        this.requisition = owningRequisition;
    }

    /**
     * Creates a line with a frozen snapshot of the supplied catalogue values.
     *
     * @param lineNumber           the 1-based ordinal within the document
     * @param itemId               the inventory catalogue item id
     * @param itemCode             the item code at request time
     * @param itemName             the item name at request time
     * @param uomCode              the unit of measure at request time
     * @param unitId               the unit master data id, may be null
     * @param requestedQuantity    the quantity requested, must be positive
     * @param estimatedUnitPrice   the planning price, must not be negative
     * @param taxPercentage        the tax percentage, 0 to 100
     * @param lineRemarks          free text remarks, may be null
     * @return a new unsaved line in {@link PurchaseRequisitionLineStatus#REQUESTED}
     */
    public static PurchaseRequisitionLine create(Integer lineNumber, Long itemId, String itemCode,
                                                 String itemName, String uomCode, Long unitId,
                                                 BigDecimal requestedQuantity,
                                                 BigDecimal estimatedUnitPrice,
                                                 BigDecimal taxPercentage, String lineRemarks) {

        validateRequestedQuantity(requestedQuantity);
        validateUnitPrice(estimatedUnitPrice);
        validatePercentage(taxPercentage, "Tax rate");

        return PurchaseRequisitionLine.builder()
                .lineNumber(lineNumber)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .unitId(unitId)
                .requestedQuantity(requestedQuantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .approvedQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE))
                .orderedQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE))
                .estimatedUnitPrice(estimatedUnitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .taxRate(taxPercentage.setScale(2, RoundingMode.HALF_UP))
                .remarks(lineRemarks)
                .status(PurchaseRequisitionLineStatus.REQUESTED)
                .build();
    }

    /**
     * Updates the demand on an editable line.
     *
     * @param quantity      the new requested quantity, must be positive
     * @param unitPrice     the revised planning price, must not be negative
     * @param taxPercentage the revised tax percentage, 0 to 100
     * @param lineRemarks   revised remarks
     */
    public void changeDemand(BigDecimal quantity, BigDecimal unitPrice,
                             BigDecimal taxPercentage, String lineRemarks) {

        validateRequestedQuantity(quantity);
        validateUnitPrice(unitPrice);
        validatePercentage(taxPercentage, "Tax rate");

        this.requestedQuantity = quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.estimatedUnitPrice = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.taxRate = taxPercentage.setScale(2, RoundingMode.HALF_UP);
        this.remarks = lineRemarks;
    }

    /**
     * Replaces everything about this line that a requester is allowed to change.
     *
     * <p>Used when a draft or returned requisition is edited in place: the row is
     * reused rather than deleted and reinserted, which keeps its identity stable
     * and avoids the unique-constraint collision that orphan replacement runs
     * into.
     *
     * <p><strong>Why the grants are cleared.</strong> The document is editable, so
     * anything previously granted on this line is a proposal the requester is now
     * changing the terms of, not a decision that survives the edit. Leaving the
     * old grant in place would let a requester who edits an approved quantity
     * from 100 down to 10 keep a line reading as fully approved, and would carry a
     * stale grant into the next approval round where the reduce-only rule then
     * refuses to correct it.
     *
     * @param itemId         the inventory catalogue item id
     * @param itemCode       the item code at request time
     * @param itemName       the item name at request time
     * @param uomCode        the unit of measure
     * @param unitId         the unit master data id, may be null
     * @param quantity       the revised requested quantity, must be positive
     * @param unitPrice      the revised planning price, must not be negative
     * @param taxPercentage  the revised tax percentage, 0 to 100
     * @param lineRemarks    revised remarks, may be null
     * @throws IllegalArgumentException if any value is out of range
     */
    public void revise(Long itemId, String itemCode, String itemName, String uomCode, Long unitId,
                       BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxPercentage,
                       String lineRemarks) {

        Objects.requireNonNull(itemId, "Item is required");
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Item code is required");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (uomCode == null || uomCode.isBlank()) {
            throw new IllegalArgumentException("Unit of measure is required");
        }

        changeDemand(quantity, unitPrice, taxPercentage, lineRemarks);

        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.uomCode = uomCode;
        this.unitId = unitId;
        this.approvedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);
        this.orderedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);
        this.status = PurchaseRequisitionLineStatus.REQUESTED;
    }

    /**
     * Grants a quantity against this line.
     *
     * <p>An approver may grant less than requested but never more. Allowing an
     * approval to exceed the request would let a document commit the branch to
     * something it never asked for, which defeats the purpose of the requisition
     * as a control.
     *
     * @param grantedQuantity the quantity granted, must not be negative
     * @throws IllegalArgumentException if the quantity is negative
     * @throws IllegalStateException    if it exceeds the requested quantity
     */
    public void approveQuantity(BigDecimal grantedQuantity) {
        requirePendingDecision();
        if (grantedQuantity == null || grantedQuantity.signum() < 0) {
            throw new IllegalArgumentException("Approved quantity cannot be negative");
        }
        if (grantedQuantity.compareTo(this.requestedQuantity) > 0) {
            throw new IllegalStateException(
                    "Cannot approve " + grantedQuantity.toPlainString()
                            + " on line " + lineNumber + " when only "
                            + this.requestedQuantity.toPlainString() + " was requested");
        }
        this.approvedQuantity = grantedQuantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.status = PurchaseRequisitionLineStatus.fromQuantities(
                this.requestedQuantity, this.approvedQuantity, this.orderedQuantity);
    }

    /**
     * Refuses this specific line without voiding the whole document.
     *
     * <p>Line-level rejection matters in practice: a branch asking for ten items
     * should not lose the three that are genuinely needed because one was
     * disallowed.
     */
    public void rejectLine() {
        if (this.orderedQuantity.signum() > 0) {
            throw new IllegalStateException(
                    "Cannot reject line " + lineNumber + " once it has been ordered");
        }
        this.approvedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE);
        this.status = PurchaseRequisitionLineStatus.REJECTED;
    }

    /**
     * Records that ordered quantity has been committed against this line.
     *
     * <p>Over-ordering is rejected here rather than only by the database so the
     * caller receives a message naming the line. This is the enforcement point
     * for the invariant that a company cannot commit to buy more than it
     * approved.
     *
     * @param newlyOrdered the quantity just ordered, must be positive
     * @return the new line status
     * @throws IllegalArgumentException if the quantity is not positive
     * @throws IllegalStateException    if it would exceed the approved quantity
     */
    public PurchaseRequisitionLineStatus registerOrdered(BigDecimal newlyOrdered) {
        if (newlyOrdered == null || newlyOrdered.signum() <= 0) {
            throw new IllegalArgumentException("Ordered quantity must be positive");
        }
        if (this.status == PurchaseRequisitionLineStatus.REJECTED) {
            throw new IllegalStateException("Cannot order against rejected line " + lineNumber);
        }
        BigDecimal newTotal = this.orderedQuantity.add(newlyOrdered);
        if (newTotal.compareTo(this.approvedQuantity) > 0) {
            throw new IllegalStateException(
                    "Ordering " + newlyOrdered.toPlainString() + " would exceed the approved quantity "
                            + this.approvedQuantity.toPlainString() + " on line " + lineNumber);
        }
        this.orderedQuantity = newTotal.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.status = PurchaseRequisitionLineStatus.fromQuantities(
                this.requestedQuantity, this.approvedQuantity, this.orderedQuantity);
        return this.status;
    }

    /**
     * Withdraws this line. The row is kept and flagged rather than deleted so
     * line numbering and the audit trail stay intact.
     *
     * @throws IllegalStateException if any quantity has already been ordered
     */
    public void cancelLine() {
        if (this.orderedQuantity.signum() > 0) {
            throw new IllegalStateException(
                    "Cannot cancel line " + lineNumber + " once it has been ordered");
        }
        this.status = PurchaseRequisitionLineStatus.CANCELLED;
    }

    /**
     * Returns the net value of this line at the <em>requested</em> quantity.
     *
     * <p>This is what the header stores in its {@code estimated_*} columns and
     * what the approval-policy resolver is measured against. It must be driven
     * by {@code requestedQuantity}: before approval every line has a zero
     * approved quantity, so a total derived from approvals would read as zero
     * for every draft, and the amount-band policy would resolve every
     * requisition - whatever its value - against the lowest approval band.
     *
     * @return requested quantity times planning price, excluding tax
     */
    public BigDecimal requestedNetAmount() {
        return netAmount(this.requestedQuantity);
    }

    /**
     * Returns the tax due on this line at the requested quantity.
     *
     * @return the requested net amount multiplied by the tax rate
     */
    public BigDecimal requestedTaxAmount() {
        return taxAmount(requestedNetAmount());
    }

    /**
     * Returns the gross value of this line at the requested quantity.
     *
     * @return requested net amount plus tax
     */
    public BigDecimal requestedGrossAmount() {
        return requestedNetAmount().add(requestedTaxAmount());
    }

    /**
     * Returns the net value of this line at the approved quantity.
     *
     * <p>Driven by {@code approvedQuantity}, not {@code requestedQuantity}, on
     * purpose: only granted demand should count towards a budget envelope. A
     * requester asking for 100 who is granted 60 must not consume budget for
     * 100.
     *
     * @return approved quantity times planning price, excluding tax
     */
    public BigDecimal approvedNetAmount() {
        return netAmount(this.approvedQuantity);
    }

    /**
     * Returns the tax due on this line at the approved quantity.
     *
     * @return the approved net amount multiplied by the tax rate
     */
    public BigDecimal approvedTaxAmount() {
        return taxAmount(approvedNetAmount());
    }

    /**
     * Returns the gross value of this line at the approved quantity.
     *
     * <p>Gross, not net, because the header's {@code approvedTotalAmount} is a
     * gross figure. Reporting a net committed total against a gross estimated
     * total would understate budget consumption by exactly the tax.
     *
     * @return approved net amount plus tax
     */
    public BigDecimal approvedGrossAmount() {
        return approvedNetAmount().add(approvedTaxAmount());
    }

    private BigDecimal netAmount(BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE);
        }
        return quantity.multiply(this.estimatedUnitPrice)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal taxAmount(BigDecimal net) {
        return net.multiply(this.taxRate)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns whether this line still contributes to the header totals.
     *
     * @return {@code false} for rejected and cancelled lines
     */
    public boolean isCounted() {
        return this.status.isCounted();
    }

    /**
     * Returns whether any approved quantity is still un-ordered.
     *
     * @return {@code true} when ordered quantity is below approved quantity
     */
    public boolean hasOutstandingQuantity() {
        return isCounted() && this.orderedQuantity.compareTo(this.approvedQuantity) < 0;
    }

    /**
     * Returns whether this line can still take an ordering decision.
     *
     * @return {@code true} while the line is not rejected or cancelled
     */
    public boolean isOrderable() {
        return this.status.isOrderable();
    }

    /**
     * Returns the shortfall between what was asked for and what was granted.
     *
     * @return requested minus approved, never negative
     */
    public BigDecimal shortfallQuantity() {
        return this.requestedQuantity.subtract(this.approvedQuantity).max(BigDecimal.ZERO);
    }


    private void requirePendingDecision() {
        if (this.status == PurchaseRequisitionLineStatus.REJECTED
                || this.status == PurchaseRequisitionLineStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Line " + lineNumber + " is " + this.status + " and cannot be approved");
        }
    }

    private static void validateRequestedQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Requested quantity must be greater than zero");
        }
    }

    private static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Estimated unit price cannot be negative");
        }
    }

    private static void validatePercentage(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        if (value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(label + " must be between 0 and 100");
        }
    }
}
