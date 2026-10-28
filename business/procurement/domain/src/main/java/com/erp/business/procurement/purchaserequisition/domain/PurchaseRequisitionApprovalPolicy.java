package com.erp.business.procurement.purchaserequisition.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * Company configuration describing who must approve a requisition at a given
 * value, for a given branch.
 *
 * <p><strong>This is not part of the requisition aggregate.</strong> It is shared
 * configuration read by many documents, so it lives outside the aggregate
 * entirely and is never cascaded from a requisition. Coupling policy rows to a
 * document would mean saving a requisition could rewrite the approval rules for
 * the entire company.
 *
 * <p><strong>Amount bands.</strong> Each row covers
 * {@code [thresholdMinAmount, thresholdMaxAmount]}, where a null maximum means
 * open-ended. A small requisition falls into the first band, a large one into
 * the last, and the chain grows with value: that is the control that makes a
 * branch unable to commit meaningful spend alone.
 *
 * <p><strong>Branch policy overrides company default.</strong> Resolution is
 * branch policy first, company default as fallback. This lets a large branch
 * require an extra sign-off that a small branch does not, without every small
 * branch having to duplicate the company rule.
 *
 * <p><strong>Exactly one assignee.</strong> A row names either an approver user
 * or an approver role, never both and never neither, enforced at construction.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisition_approval_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pr_policies_scope_branch_sequence",
                columnNames = {"tenant_id", "company_id", "branch_id", "sequence_no"}
        ),
        indexes = {
                @Index(name = "idx_pr_policies_lookup",
                        columnList = "tenant_id, company_id, branch_id, sequence_no")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisitionApprovalPolicy extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * Branch this policy applies to, or {@code null} for a company-wide default.
     *
     * <p>Nullable rather than duplicated per branch on purpose: materialising the
     * company default into every branch row would mean a change to the default
     * had to be applied N times and could silently diverge between branches.
     */
    @Column(name = "branch_id")
    private Long branchId;

    /** 1-based position in the chain this policy step occupies. */
    @NotNull(message = "Sequence number is required")
    @Positive(message = "Sequence number must be positive")
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /** Inclusive lower bound of the amount band. Zero for the first step. */
    @NotNull(message = "Minimum threshold is required")
    @Column(name = "threshold_min_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal thresholdMinAmount = BigDecimal.ZERO.setScale(MONEY_SCALE);

    /** Exclusive upper bound, or {@code null} for the final open-ended step. */
    @Column(name = "threshold_max_amount", precision = 19, scale = 4)
    private BigDecimal thresholdMaxAmount;

    @Column(name = "approver_role_id")
    private Long approverRoleId;

    @Column(name = "approver_user_id")
    private Long approverUserId;

    @Column(name = "approver_role", length = 100)
    private String approverRole;

    /** Soft enable and disable. A disabled rule is ignored by the resolver. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Creates a branch-specific policy step assigned to a role.
     *
     * @param branchId   the branch, or null for a company-wide default
     * @param sequenceNo the 1-based position
     * @param minAmount  the inclusive lower bound, must not be negative
     * @param maxAmount  the exclusive upper bound, or null for open-ended
     * @param roleId     the approver role id
     * @param roleName   the role name for display
     * @return a new unsaved policy
     */
    public static PurchaseRequisitionApprovalPolicy forRole(
            Long branchId, Integer sequenceNo, BigDecimal minAmount, BigDecimal maxAmount,
            Long roleId, String roleName) {

        if (roleId == null) {
            throw new IllegalArgumentException("An approver role is required");
        }
        return build(branchId, sequenceNo, minAmount, maxAmount, roleId, null, roleName);
    }

    /**
     * Creates a branch-specific policy step assigned to a named approver.
     *
     * @param branchId   the branch, or null for a company-wide default
     * @param sequenceNo the 1-based position
     * @param minAmount  the inclusive lower bound, must not be negative
     * @param maxAmount  the exclusive upper bound, or null for open-ended
     * @param userId     the approver user id
     * @param roleName   the approver's role name for display
     * @return a new unsaved policy
     */
    public static PurchaseRequisitionApprovalPolicy forUser(
            Long branchId, Integer sequenceNo, BigDecimal minAmount, BigDecimal maxAmount,
            Long userId, String roleName) {

        if (userId == null) {
            throw new IllegalArgumentException("An approver is required");
        }
        return build(branchId, sequenceNo, minAmount, maxAmount, null, userId, roleName);
    }

    /**
     * Returns whether this policy applies to a requisition of the given value.
     *
     * @param amount the requisition value
     * @return {@code true} when the amount falls inside this band
     */
    public boolean appliesTo(BigDecimal amount) {
        if (!active || amount == null) {
            return false;
        }
        if (amount.compareTo(this.thresholdMinAmount) < 0) {
            return false;
        }
        return this.thresholdMaxAmount == null
                || amount.compareTo(this.thresholdMaxAmount) < 0;
    }

    /**
     * Returns whether this policy is a company-wide default.
     *
     * @return {@code true} when no branch is set
     */
    public boolean isCompanyDefault() {
        return this.branchId == null;
    }

    /**
     * Disables this policy without deleting it.
     *
     * <p>Soft disable rather than delete: a rule that once governed approvals is
     * part of the audit story, even after the company has decided it no longer
     * applies.
     */
    public void deactivate() {
        this.active = false;
    }

    private static PurchaseRequisitionApprovalPolicy build(
            Long branchId, Integer sequenceNo, BigDecimal minAmount, BigDecimal maxAmount,
            Long roleId, Long userId, String roleName) {

        Objects.requireNonNull(sequenceNo, "Sequence number is required");
        if (sequenceNo <= 0) {
            throw new IllegalArgumentException("Sequence number must be positive");
        }
        if (roleId != null && userId != null) {
            throw new IllegalArgumentException(
                    "A policy step names either a role or a user, never both");
        }
        if (minAmount == null || minAmount.signum() < 0) {
            throw new IllegalArgumentException("Minimum threshold cannot be negative");
        }
        if (maxAmount != null) {
            if (maxAmount.signum() < 0) {
                throw new IllegalArgumentException("Maximum threshold cannot be negative");
            }
            if (maxAmount.compareTo(minAmount) <= 0) {
                throw new IllegalArgumentException(
                        "Maximum threshold " + maxAmount.toPlainString()
                                + " must be greater than the minimum " + minAmount.toPlainString());
            }
        }

        return PurchaseRequisitionApprovalPolicy.builder()
                .branchId(branchId)
                .sequenceNo(sequenceNo)
                .thresholdMinAmount(minAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .thresholdMaxAmount(maxAmount == null
                        ? null
                        : maxAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .approverRoleId(roleId)
                .approverUserId(userId)
                .approverRole(roleName)
                .active(true)
                .build();
    }
}
