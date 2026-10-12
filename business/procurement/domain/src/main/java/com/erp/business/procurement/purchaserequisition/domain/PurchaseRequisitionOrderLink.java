package com.erp.business.procurement.purchaserequisition.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
import java.util.Objects;

/**
 * Traceability record joining approved requisition demand to a purchase order.
 *
 * <p><strong>Why a link table rather than a foreign key.</strong> The real
 * cardinalities are not one-to-one:
 * <ul>
 *   <li>one requisition produces <em>many</em> purchase orders, because a buyer
 *       may consolidate demand from several branches onto one order, or split
 *       one line across suppliers;</li>
 *   <li>one requisition line appears on <em>many</em> purchase order lines,
 *       because a single item may be awarded partly to one supplier and partly
 *       to another.</li>
 * </ul>
 * A {@code purchase_order_id} column on the requisition could express neither.
 *
 * <p><strong>Why the quantity lives on the link.</strong> Each link records how
 * much <em>this</em> order contributes to <em>this</em> line. The invariant
 * {@code SUM(link.orderedQuantity) == line.orderedQuantity} is therefore
 * checkable at the link level rather than having to be inferred, and a partial
 * award is a first-class record rather than a subtraction.
 *
 * <p><strong>Why there is no JPA relation to {@code PurchaseOrder}.</strong> A
 * mapped {@code @ManyToOne} would create a bidirectional aggregate and, in
 * practice, a circular dependency between the requisition and purchase order
 * packages. The reference is by id and the application resolves it, keeping the
 * two document types independently loadable and independently versioned.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisition_order_links",
        indexes = {
                @Index(name = "idx_pr_order_links_requisition",
                        columnList = "purchase_requisition_id"),
                @Index(name = "idx_pr_order_links_order",
                        columnList = "tenant_id, company_id, purchase_order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisitionOrderLink extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int QUANTITY_SCALE = 6;

    /**
     * The requisition this link belongs to.
     *
     * <p>Denormalised alongside the line reference on purpose: the dominant query
     * is "everything ordered against this document", which should not have to
     * join through the line table.
     */
    @NotNull(message = "Requisition is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseRequisition requisition;

    /**
     * The specific line being ordered.
     *
     * <p>Stored as an id rather than a relation so that reading the link does not
     * force the line collection to initialise.
     */
    @NotNull(message = "Requisition line is required")
    @Column(name = "purchase_requisition_line_id", nullable = false, updatable = false)
    private Long purchaseRequisitionLineId;

    /**
     * The purchase order this demand was awarded to.
     *
     * <p>A plain id, not a mapped relation: the purchase order is a separate
     * document type and modelling a relation here would couple the two
     * aggregates.
     */
    @NotNull(message = "Purchase order is required")
    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    /** The purchase order line, when the award split is known. */
    @Column(name = "purchase_order_line_id")
    private Long purchaseOrderLineId;

    /** Quantity this link contributes to the requisition line. Must be positive. */
    @NotNull(message = "Ordered quantity is required")
    @Column(name = "ordered_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal orderedQuantity;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "linked_by", length = 100)
    private String linkedBy;

    /**
     * Creates a link recording a quantity awarded to a purchase order.
     *
     * @param requisition       the owning requisition
     * @param lineId            the requisition line being ordered
     * @param orderId           the purchase order receiving the award
     * @param orderLineId       the purchase order line, may be null if not yet split
     * @param quantity          the quantity awarded, must be positive
     * @param actor             the linking user id
     * @return a new unsaved link
     */
    public static PurchaseRequisitionOrderLink award(
            PurchaseRequisition requisition, Long lineId, Long orderId,
            Long orderLineId, BigDecimal quantity, String actor) {

        Objects.requireNonNull(requisition, "Requisition is required");
        Objects.requireNonNull(lineId, "Requisition line is required");
        Objects.requireNonNull(orderId, "Purchase order is required");

        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Awarded quantity must be greater than zero");
        }

        return PurchaseRequisitionOrderLink.builder()
                .requisition(requisition)
                .purchaseRequisitionLineId(lineId)
                .purchaseOrderId(orderId)
                .purchaseOrderLineId(orderLineId)
                .orderedQuantity(quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                .linkedAt(Instant.now())
                .linkedBy(actor)
                .build();
    }

    /**
     * Returns whether this link is a full or partial award of its line.
     *
     * @param approvedQuantity the line's approved quantity
     * @return {@code true} when this link alone satisfies the approval
     */
    public boolean fullyAwards(BigDecimal approvedQuantity) {
        return approvedQuantity != null
                && this.orderedQuantity.compareTo(approvedQuantity) >= 0;
    }
}
