package com.erp.business.procurement.purchaseorder.domain;

import com.erp.business.procurement.shared.domain.TenantScopedEntity;
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
import java.time.LocalDate;

/**
 * One line of a purchase order: what was bought, how much, at what price.
 *
 * <p><strong>Why the item attributes are duplicated here.</strong> This entity
 * stores {@code itemCode}, {@code itemName} and {@code uomCode} alongside the
 * {@code itemId} reference. A purchase order is a contract with a supplier: if
 * an item is later renamed, re-coded, re-tariffed or discontinued, the document
 * that was legally issued must continue to show what was actually ordered. The
 * live catalogue values are read from the inventory service at reporting time;
 * they are never written back into this snapshot.
 *
 * <p><strong>Why {@code lineAmount} is generated in the database.</strong>
 * {@code quantity * unit_price * (1 - discount/100)} is expressed as a
 * {@code GENERATED ALWAYS AS ... STORED} column in PostgreSQL. A Java-side
 * computed field can be forgotten on one of the many code paths that mutate a
 * line; a generated column is recalculated by the database on every write, so
 * the stored amount cannot drift from its inputs.
 *
 * <p>Lines are only mutable while the parent order is in {@link
 * PurchaseOrderStatus#DRAFT}. {@link PurchaseOrder} owns that rule so it cannot
 * be bypassed by calling a setter directly.
 */
@Entity
@Table(
        name = "purchase_order_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_order_lines_order_number",
                columnNames = {"purchase_order_id", "line_number"}
        ),
        indexes = {
                @Index(name = "idx_purchase_order_lines_order",
                        columnList = "purchase_order_id, line_number"),
                @Index(name = "idx_purchase_order_lines_order_item",
                        columnList = "tenant_id, company_id, purchase_order_id, item_id"),
                @Index(name = "idx_purchase_order_lines_pending",
                        columnList = "tenant_id, company_id, item_id, purchase_order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseOrderLine extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** Stable ordinal within the document, 1-based. Never re-used after issue. */
    @NotNull(message = "Line number is required")
    @Positive(message = "Line number must be positive")
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** Reference into the inventory catalogue. Not a database foreign key: cross-service. */
    @NotNull(message = "Item is required")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Frozen snapshot of the item at issue time. */
    @NotBlank(message = "Item code is required")
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @NotBlank(message = "Unit of measure is required")
    @Column(name = "uom_code", nullable = false, length = 32)
    private String uomCode;

    @NotNull(message = "Quantity is required")
    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @NotNull(message = "Discount percentage is required")
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @NotNull(message = "Tax rate is required")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    /**
     * Net line amount excluding tax.
     *
     * <p>Mapped as read-only: the database generates it. Hibernate is told not
     * to insert or update the column, so a stale in-memory value can never be
     * written back over the database's own calculation. Refresh the entity
     * after a line mutation to read the authoritative value.
     */
    @Column(name = "line_amount", precision = 19, scale = 4, insertable = false, updatable = false)
    private BigDecimal lineAmount;

    @NotNull(message = "Received quantity is required")
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @NotNull(message = "Line status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderLineStatus status = PurchaseOrderLineStatus.OPEN;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Owning order. Set by {@link PurchaseOrder#addLine(PurchaseOrderLine)};
     * a line is never attached to an order by the caller directly.
     *
     * <p>Mapped without a cascade and without {@code insertable}: the header
     * owns the lifecycle, so saving the header persists the lines.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;

    /**
     * Links this line to its parent order. Package-private on purpose: the
     * aggregate root is the only writer of this field.
     *
     * @param order the owning order
     */
    void attachTo(PurchaseOrder order) {
        this.purchaseOrder = order;
    }

    /**
     * Creates a line with a frozen snapshot of the supplied catalogue values.
     *
     * @param lineNumber     the 1-based ordinal within the document
     * @param itemId         the inventory catalogue item id
     * @param itemCode       the item code at issue time
     * @param itemName       the item name at issue time
     * @param uomCode        the unit of measure at issue time
     * @param quantity       the ordered quantity, must be positive
     * @param unitPrice      the unit price, must not be negative
     * @param discountRate   the line discount percentage, 0 to 100
     * @param taxRate        the tax percentage, 0 to 100
     * @return a new unsaved line
     */
    public static PurchaseOrderLine create(Integer lineNumber, Long itemId, String itemCode,
                                           String itemName, String uomCode, BigDecimal quantity,
                                           BigDecimal unitPrice, BigDecimal discountRate,
                                           BigDecimal taxRate) {
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
        validatePercentage(discountRate, "Discount");
        validatePercentage(taxRate, "Tax rate");

        return PurchaseOrderLine.builder()
                .lineNumber(lineNumber)
                .itemId(itemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .uomCode(uomCode)
                .quantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .unitPrice(unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .discountPercentage(discountRate.setScale(2, RoundingMode.HALF_UP))
                .taxRate(taxRate.setScale(2, RoundingMode.HALF_UP))
                .receivedQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .status(PurchaseOrderLineStatus.OPEN)
                .build();
    }

    /**
     * Updates the commercial terms of a draft line.
     *
     * @param quantity      the new ordered quantity, must be positive
     * @param unitPrice     the new unit price, must not be negative
     * @param discountRate  the new discount percentage, 0 to 100
     * @param taxRate       the tax percentage, 0 to 100
     * @param expectedDate  the revised expected delivery date, may be {@code null}
     * @param lineNotes     revised free text notes, may be {@code null}
     */
    public void changeTerms(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discountRate,
                            BigDecimal taxRate, LocalDate expectedDate, String lineNotes) {
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);
        validatePercentage(discountRate, "Discount");
        validatePercentage(taxRate, "Tax rate");

        if (this.receivedQuantity.signum() > 0
                && quantity.compareTo(this.receivedQuantity) < 0) {
            throw new IllegalStateException(
                    "Cannot reduce the ordered quantity below the already received quantity "
                            + this.receivedQuantity.toPlainString());
        }
        this.quantity = quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.unitPrice = unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.discountPercentage = discountRate.setScale(2, RoundingMode.HALF_UP);
        this.taxRate = taxRate.setScale(2, RoundingMode.HALF_UP);
        this.expectedDeliveryDate = expectedDate;
        this.notes = lineNotes;
    }

    /**
     * Records a goods receipt against this line and re-derives the line status.
     *
     * <p>Over-receipt is rejected here rather than only by the database
     * constraint so the caller receives a message naming the line.
     *
     * @param receivedQuantity the quantity just received, must be positive
     * @return the new line status
     * @throws IllegalArgumentException  if the quantity is not positive
     * @throws IllegalStateException     if it would exceed the ordered quantity
     */
    public PurchaseOrderLineStatus receive(BigDecimal receivedQuantity) {
        if (receivedQuantity == null || receivedQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Received quantity must be positive");
        }
        if (this.status == PurchaseOrderLineStatus.CANCELLED) {
            throw new IllegalStateException("Cannot receive against a cancelled line");
        }
        BigDecimal newTotal = this.receivedQuantity.add(receivedQuantity);
        if (newTotal.compareTo(this.quantity) > 0) {
            throw new IllegalStateException(
                    "Receiving " + receivedQuantity.toPlainString()
                            + " would exceed the ordered quantity " + this.quantity.toPlainString()
                            + " on line " + this.lineNumber);
        }
        this.receivedQuantity = newTotal.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        this.status = PurchaseOrderLineStatus.fromReceivedQuantity(this.quantity, this.receivedQuantity);
        return this.status;
    }

    /**
     * Withdraws this line. The row is kept and flagged rather than deleted so
     * that line numbering and the audit trail stay intact.
     */
    public void cancelLine() {
        if (this.receivedQuantity.signum() > 0) {
            throw new IllegalStateException("Cannot cancel a line that has received goods");
        }
        this.status = PurchaseOrderLineStatus.CANCELLED;
    }

    /**
     * Returns the tax amount due on this line.
     *
     * @return the line amount multiplied by the tax rate, rounded to the money scale
     */
    public BigDecimal taxAmount() {
        BigDecimal net = netAmount();
        return net.multiply(this.taxRate)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns the net line amount computed in Java.
     *
     * <p>This mirrors the generated {@code line_amount} column so that header
     * total recalculation works on a freshly mutated entity that has not yet
     * been flushed and re-read.
     *
     * @return quantity times unit price net of the line discount
     */
    public BigDecimal netAmount() {
        BigDecimal gross = this.quantity.multiply(this.unitPrice);
        BigDecimal factor = ONE_HUNDRED.subtract(this.discountPercentage)
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        return gross.multiply(factor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns whether this line still contributes to the document totals.
     *
     * @return {@code false} only for cancelled lines
     */
    public boolean isCounted() {
        return this.status != PurchaseOrderLineStatus.CANCELLED;
    }

    /**
     * Returns whether any quantity is still outstanding on this line.
     *
     * @return {@code true} when the received quantity is below the ordered quantity
     */
    public boolean hasOutstandingQuantity() {
        return isCounted() && this.receivedQuantity.compareTo(this.quantity) < 0;
    }

    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
    }

    private static void validatePercentage(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " percentage is required");
        }
        if (value.signum() < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(label + " percentage must be between 0 and 100");
        }
    }
}
