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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * One immutable entry in a requisition's lifecycle audit trail.
 *
 * <p><strong>Append-only.</strong> There is no method on this class that
 * mutates an existing entry, and the owning
 * {@link PurchaseRequisition} maps its history collection with
 * {@code orphanRemoval = false} specifically so that a row cannot be silently
 * dropped by detaching it. A procurement document is a legal record: "who moved
 * this to approved, when, and why" must remain answerable for as long as the
 * document is retained, independent of any later edit to the document itself.
 *
 * <p>Status is recorded as the resolved enum rather than the raw column string,
 * so the trail cannot be written with a value the lifecycle does not permit.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisition_status_history",
        indexes = {
                @Index(name = "idx_purchase_requisition_status_history_requisition",
                        columnList = "purchase_requisition_id, changed_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisitionStatusHistory extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /** Null only for the entry that records creation of the document. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private PurchaseRequisitionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private PurchaseRequisitionStatus toStatus;

    /** The acting user id. */
    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** Why the transition happened. Required for rejection, return and cancellation. */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Owning requisition. Package-private writer only, via
     * {@link PurchaseRequisitionStatusHistory#record}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseRequisition requisition;

    /**
     * Links this entry to its parent requisition. Package-private on purpose:
     * the aggregate root is the only writer of this field.
     *
     * @param owningRequisition the owning requisition
     */
    void attachTo(PurchaseRequisition owningRequisition) {
        this.requisition = owningRequisition;
    }

    /**
     * Records a status transition.
     *
     * <p>The audit trail is append-only and refuses to record an edge the
     * transition table does not permit, so the history of a document can never
     * describe a journey the document itself would not have allowed.
     *
     * @param from    the previous state, null when recording document creation
     * @param to      the new state
     * @param actor   the acting user id
     * @param because the reason, required for rejection and cancellation
     * @return a new unsaved history entry
     */
    public static PurchaseRequisitionStatusHistory record(
            PurchaseRequisitionStatus from, PurchaseRequisitionStatus to,
            String actor, String because) {

        Objects.requireNonNull(to, "A target status is required");

        boolean reasonRequired = to == PurchaseRequisitionStatus.REJECTED
                || to == PurchaseRequisitionStatus.CANCELLED;
        if (reasonRequired && (because == null || because.isBlank())) {
            throw new IllegalArgumentException(
                    "A reason is required to record a transition to " + to);
        }
        if (from != null && !from.canTransitionTo(to)) {
            throw new IllegalStateException(
                    "Cannot record a transition from " + from + " to " + to);
        }

        return PurchaseRequisitionStatusHistory.builder()
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actor)
                .changedAt(Instant.now())
                .reason(because)
                .build();
    }

    /**
     * Returns whether this entry records the creation of the document.
     *
     * @return {@code true} when there is no previous state
     */
    public boolean isCreation() {
        return this.fromStatus == null;
    }
}
