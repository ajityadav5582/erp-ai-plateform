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
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/**
 * One materialised step of a requisition's approval chain.
 *
 * <p><strong>Why the chain is snapshotted rather than resolved live.</strong>
 * The rows are written at submission time from the effective
 * {@code PurchaseRequisitionApprovalPolicy}. If the chain were resolved from the
 * policy at approval time, an administrator editing a threshold while a
 * requisition sat in the queue would retroactively change or skip a step on a
 * document that had already been submitted. That is an audit failure, not a
 * convenience problem. Snapshotting freezes the obligation that existed when the
 * document entered the workflow.
 *
 * <p><strong>Why a step is skipped rather than deleted.</strong> Amount-band
 * policies are evaluated in sequence and most do not apply to every document.
 * Inapplicable steps are marked {@link ApprovalAction#SKIPPED} and kept, so the
 * stored chain still reads as the complete policy that was applied. Deleting
 * them would leave an auditor unable to distinguish a deliberate skip from a
 * missing configuration.
 *
 * <p><strong>How a step is addressed.</strong> There are three forms, and a step
 * may never mix two of them. Naming a user gives accountability; naming a role
 * gives flexibility when people change; naming neither addresses the step to
 * whoever holds the approve permission, which is what lets a company work before
 * an administrator has configured any approval policy at all. Allowing a step
 * to name both a user and a role would create two people who each believe the
 * approval is theirs.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "purchase_requisition_approvals",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_purchase_requisition_approvals_requisition_sequence",
                columnNames = {"purchase_requisition_id", "sequence_no"}
        ),
        indexes = {
                @Index(name = "idx_purchase_requisition_approvals_pending",
                        columnList = "purchase_requisition_id, sequence_no"),
                @Index(name = "idx_purchase_requisition_approvals_assignee",
                        columnList = "tenant_id, company_id, approver_id, action")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequisitionApproval extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    private static final int MONEY_SCALE = 4;

    /**
     * The actor recorded against an approval that nobody personally gave.
     *
     * <p>An automatic approval still lands in {@code actedBy}, and the obvious
     * shortcut is to write the submitter's own id there - the requester submitted
     * it, so they "approved" it. That is a lie of exactly the kind this table
     * exists to prevent: a reader of the history would see a named approver and
     * conclude a second person looked at the spend. A reserved marker makes the
     * absence of a human visible in the row itself, without inventing a
     * pseudo-user that could collide with a real account id.
     */
    public static final String SYSTEM_ACTOR = "SYSTEM";

    /** The label written on a step that needed no human approver. */
    public static final String AUTOMATIC_APPROVAL_LABEL = "Automatic (no approval required)";

    /**
     * The reason recorded against every automatically approved step.
     *
     * <p>Wording matters here. It says which setting produced the approval, not
     * merely that one happened, because the only question an auditor has about an
     * approval nobody signed is whether the company had configured the workflow
     * that way at the time.
     */
    public static final String AUTO_APPROVAL_COMMENT =
            "Approved automatically on submission: this company's workflow settings do not "
                    + "require approval for purchase requisitions.";

    /** 1-based position in the chain. Strictly increasing within a document. */
    @NotNull(message = "Sequence number is required")
    @Positive(message = "Sequence number must be positive")
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /**
     * Named approver. Mutually exclusive with {@link #approverRoleId}; null on a
     * step addressed to whoever holds the approve permission.
     */
    @Column(name = "approver_id")
    private Long approverId;

    /**
     * Approver role. Mutually exclusive with {@link #approverId}; null on a step
     * addressed to whoever holds the approve permission.
     */
    @Column(name = "approver_role_id")
    private Long approverRoleId;

    /**
     * Human readable role or person name, frozen at submission for display.
     *
     * <p>Always populated, including on a permission-addressed step, where it
     * carries the description of who may act rather than a role to match. The
     * details screen renders this value, so a null here would show an approver
     * looking at a blank step.
     */
    @Column(name = "approver_role", length = 100)
    private String approverRole;

    /** Amount band this step governs. */
    @Column(name = "amount_threshold", precision = 19, scale = 4)
    private BigDecimal amountThreshold;

    /** Branch this step was configured for; null means a company-wide default. */
    @Column(name = "policy_branch_id")
    private Long policyBranchId;

    @NotNull(message = "Approval action is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    @Builder.Default
    private ApprovalAction action = ApprovalAction.PENDING;

    /** Null while {@link #action} is {@link ApprovalAction#PENDING}. */
    @Column(name = "acted_by", length = 100)
    private String actedBy;

    /** Null while {@link #action} is {@link ApprovalAction#PENDING}. */
    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "comments", length = 500)
    private String comments;

    /**
     * Owning requisition. Set by {@link PurchaseRequisition#submit}; never
     * assigned by the caller directly.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false, updatable = false)
    @ToString.Exclude
    private PurchaseRequisition requisition;

    /**
     * Links this step to its parent requisition. Package-private on purpose:
     * the aggregate root is the only writer of this field.
     *
     * @param owningRequisition the owning requisition
     */
    void attachTo(PurchaseRequisition owningRequisition) {
        this.requisition = owningRequisition;
    }

    /**
     * Creates a pending step assigned to a named approver.
     *
     * @param sequenceNo     the 1-based position in the chain
     * @param approverId     the approver user id
     * @param approverRole   the approver's role name for display, may be null
     * @param amountThreshold the amount band this step governs, may be null
     * @param policyBranchId the branch this policy came from, null for company default
     * @return a new unsaved pending step
     */
    public static PurchaseRequisitionApproval assignedToUser(
            Integer sequenceNo, Long approverId, String approverRole,
            BigDecimal amountThreshold, Long policyBranchId) {

        requireSequence(sequenceNo);
        Objects.requireNonNull(approverId, "An approver is required for an approval step");

        return PurchaseRequisitionApproval.builder()
                .sequenceNo(sequenceNo)
                .approverId(approverId)
                .approverRole(approverRole)
                .amountThreshold(scaleMoney(amountThreshold))
                .policyBranchId(policyBranchId)
                .action(ApprovalAction.PENDING)
                .build();
    }

    /**
     * Creates a pending step assigned to a role.
     *
     * @param sequenceNo      the 1-based position in the chain
     * @param approverRoleId  the approver role id
     * @param approverRole    the role name for display
     * @param amountThreshold the amount band this step governs, may be null
     * @param policyBranchId  the branch this policy came from, null for company default
     * @return a new unsaved pending step
     */
    public static PurchaseRequisitionApproval assignedToRole(
            Integer sequenceNo, Long approverRoleId, String approverRole,
            BigDecimal amountThreshold, Long policyBranchId) {

        requireSequence(sequenceNo);
        Objects.requireNonNull(approverRoleId, "An approver role is required for an approval step");

        return PurchaseRequisitionApproval.builder()
                .sequenceNo(sequenceNo)
                .approverRoleId(approverRoleId)
                .approverRole(approverRole)
                .amountThreshold(scaleMoney(amountThreshold))
                .policyBranchId(policyBranchId)
                .action(ApprovalAction.PENDING)
                .build();
    }

    /**
     * Creates a pending step addressed to whoever holds the approve permission.
     *
     * <p>This is the third addressing form, alongside
     * {@link #assignedToUser} and {@link #assignedToRole}. It exists because a
     * company that has configured no approval policy still has to be able to run
     * its requisitions: refusing submission left the module unusable, since the
     * policy it refused to work without had no API through which it could be
     * created. One open step decided by anyone holding the approve permission is
     * a real control - the spend is not committed without a second pair of eyes -
     * and it degrades to named approvers as soon as a policy is configured.
     *
     * <p>Authority is <em>not</em> decided here. Whether the caller actually
     * holds the approve permission is settled by the method-level permission
     * check on the decision endpoint, which reads it from the signed token. This
     * type records who the step is addressed to; it never grants anyone
     * authority.
     *
     * @param sequenceNo      the 1-based position in the chain
     * @param amountThreshold the value of the document the step was opened for, may be null
     * @param policyBranchId  the branch that had no policy, null for a company default
     * @param description     the display label for who may act on this step, required
     * @return a new unsaved pending step
     * @throws IllegalArgumentException if the description is blank
     */
    public static PurchaseRequisitionApproval openToApprovers(
            Integer sequenceNo, BigDecimal amountThreshold, Long policyBranchId,
            String description) {

        requireSequence(sequenceNo);
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "A permission-addressed step needs a description of who may act on it; "
                            + "the details screen has nothing else to show");
        }

        return PurchaseRequisitionApproval.builder()
                .sequenceNo(sequenceNo)
                .approverRole(description)
                .amountThreshold(scaleMoney(amountThreshold))
                .policyBranchId(policyBranchId)
                .action(ApprovalAction.PENDING)
                .build();
    }

    /**
     * Creates a step marked as not applicable to this document.
     *
     * @param sequenceNo      the 1-based position in the chain
     * @param approverRole    the role name for display
     * @param amountThreshold the amount band this step governs
     * @param policyBranchId  the branch this policy came from
     * @return a new unsaved skipped step
     */
    public static PurchaseRequisitionApproval skipped(
            Integer sequenceNo, String approverRole,
            BigDecimal amountThreshold, Long policyBranchId) {

        requireSequence(sequenceNo);

        return PurchaseRequisitionApproval.builder()
                .sequenceNo(sequenceNo)
                .approverRole(approverRole)
                .amountThreshold(scaleMoney(amountThreshold))
                .policyBranchId(policyBranchId)
                .action(ApprovalAction.SKIPPED)
                .build();
    }

    /**
     * Creates a step that is already approved, because no approver was required.
     *
     * <p>This is the fourth creation form, and the only one that does not begin
     * {@link ApprovalAction#PENDING}. It exists for companies that have turned the
     * purchase-requisition approval requirement off: their requisitions still go
     * through submission, and they still get a chain on record, but no step waits
     * for a person.
     *
     * <p><strong>Why the row is written at all.</strong> The step could be left
     * absent, or left pending with nobody to action it. Both are worse. An absent
     * row makes an approved requisition indistinguishable from one that skipped
     * its chain, and a pending row leaves a document sitting in a queue that no
     * one will ever clear - a support ticket weeks later. A completed row marked
     * {@link ApprovalAction#APPROVED} with {@link #SYSTEM_ACTOR} in {@code actedBy}
     * says exactly what happened, in the same shape as every other approval, and
     * answers the question a reader will ask first: was this approved by somebody,
     * or by a setting?
     *
     * <p><strong>Why it bypasses {@link #decide}.</strong> {@code decide} guards
     * against a step being decided twice, which is meaningless for a step that is
     * never handed to anyone. The permission checks {@code decide} ultimately
     * backs onto belong to a person acting; there is no caller here, and the
     * company setting that caused this is itself permission-gated.
     *
     * @param sequenceNo      the 1-based position in the chain
     * @param amountThreshold the amount band the step would have governed, may be null
     * @param policyBranchId  the branch the policy came from, null for company default
     * @return a new unsaved, already-approved step
     * @throws IllegalArgumentException if the sequence number is not positive
     */
    public static PurchaseRequisitionApproval autoApproved(
            Integer sequenceNo, BigDecimal amountThreshold, Long policyBranchId) {

        requireSequence(sequenceNo);

        return PurchaseRequisitionApproval.builder()
                .sequenceNo(sequenceNo)
                .approverRole(AUTOMATIC_APPROVAL_LABEL)
                .amountThreshold(scaleMoney(amountThreshold))
                .policyBranchId(policyBranchId)
                .action(ApprovalAction.APPROVED)
                .actedBy(SYSTEM_ACTOR)
                .actedAt(Instant.now())
                .comments(AUTO_APPROVAL_COMMENT)
                .build();
    }

    /**
     * Returns whether this step was approved by the system rather than by a person.
     *
     * <p>Driven by {@link #SYSTEM_ACTOR} rather than by the {@code approverId}
     * being null: a permission-addressed step also names nobody, and reading it as
     * automatic would report every ordinary open-to-approvers step as automated.
     *
     * @return {@code true} when the step is approved and nobody signed it
     */
    public boolean isAutomaticallyApproved() {
        return this.action == ApprovalAction.APPROVED && SYSTEM_ACTOR.equals(this.actedBy);
    }

    /**
     * Records an approval decision on this step.
     *
     * <p>A step can be decided once. Re-deciding it would rewrite who approved
     * what and when, which is precisely the history this row exists to keep.
     *
     * @param decision the decision, must be {@link ApprovalAction#APPROVED} or
     *                 {@link ApprovalAction#REJECTED}
     * @param actor    the acting user id
     * @param comments the approver's remarks, required for a refusal
     * @throws IllegalStateException    if the step is already decided
     * @throws IllegalArgumentException if the decision is null, is not a real
     *                                  decision, or refuses without a comment
     */
    public void decide(ApprovalAction decision, String actor, String comments) {
        Objects.requireNonNull(decision, "An approval decision is required");
        if (decision == ApprovalAction.PENDING) {
            throw new IllegalArgumentException(
                    "PENDING is not a decision; use it only as the initial state");
        }
        if (decision == ApprovalAction.SKIPPED) {
            throw new IllegalArgumentException(
                    "A step cannot be skipped after submission; skipped steps are created by the "
                            + "policy resolver");
        }
        if (this.action != ApprovalAction.PENDING) {
            throw new IllegalStateException(
                    "Approval step " + sequenceNo + " is already " + this.action);
        }
        if (decision == ApprovalAction.REJECTED && (comments == null || comments.isBlank())) {
            throw new IllegalArgumentException(
                    "A comment is required to reject a requisition");
        }

        this.action = decision;
        this.actedBy = actor;
        this.actedAt = Instant.now();
        this.comments = comments;
    }

    /**
     * Returns whether this step is still awaiting a decision.
     *
     * @return {@code true} while the action is {@link ApprovalAction#PENDING}
     */
    public boolean isPending() {
        return this.action == ApprovalAction.PENDING;
    }

    /**
     * Returns whether the given user is the named assignee on this step.
     *
     * <p>Only a specific-user assignment can be matched this way. A role
     * assignment must be resolved by the application layer against the user's
     * effective roles, because the role membership lives in the identity
     * service.
     *
     * @param userId the acting user id
     * @return {@code true} when this step names that user
     */
    public boolean isAssignedTo(Long userId) {
        return this.approverId != null && this.approverId.equals(userId);
    }

    /**
     * Returns whether this step is assigned to a role rather than a person.
     *
     * @return {@code true} when a role assignment applies
     */
    public boolean isRoleAssigned() {
        return this.approverRoleId != null;
    }

    /**
     * Returns whether this step is addressed to whoever holds the approve
     * permission, rather than to a named person or a named role.
     *
     * <p>True only when the step names nobody at all, which is exactly what
     * distinguishes this form from the other two. The caller is responsible for
     * having already established that the acting user holds the approve
     * permission; this predicate only reports the shape of the step.
     *
     * @return {@code true} when the step names neither a user nor a role
     */
    public boolean isPermissionAssigned() {
        return this.approverId == null && this.approverRoleId == null;
    }

    /**
     * Returns whether the caller holds the role this step is assigned to.
     *
     * <p>This is what makes role-based approval chains usable. Without it a step
     * named against a role can never be acted on, which silently disables the
     * {@code forRole(...)} form of
     * {@link PurchaseRequisitionApprovalPolicy} - and that form is how most
     * companies actually want to configure approvals, because naming a role
     * survives an approver leaving while naming a person does not.
     *
     * <p><strong>How the match is made.</strong> Role membership is not in this
     * database, so the caller passes the acting user's effective roles as
     * resolved from the signed access token by the application layer. Two
     * candidates are compared, both case-insensitively and ignoring surrounding
     * whitespace:
     * <ul>
     *   <li>{@code approverRole} - the role name frozen onto the step at
     *       submission, which is the normal match. The name is frozen rather
     *       than resolved live precisely so an administrative rename cannot
     *       retroactively move a step off the person who owes it.</li>
     *   <li>the decimal form of {@code approverRoleId}, for deployments whose
     *       identity provider puts numeric role ids in the token rather than
     *       role names.</li>
     * </ul>
     *
     * @param callerRoles the acting user's role names, may be null or empty
     * @return {@code true} when the caller holds the assigned role
     */
    public boolean isRoleHeldBy(Collection<String> callerRoles) {
        if (!isRoleAssigned() || callerRoles == null || callerRoles.isEmpty()) {
            return false;
        }
        String assignedName = normalizeRoleName(this.approverRole);
        String assignedId = this.approverRoleId == null ? null : this.approverRoleId.toString();

        for (String candidate : callerRoles) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String role = normalizeRoleName(candidate);
            if ((assignedName != null && assignedName.equals(role))
                    || (assignedId != null && assignedId.equals(role))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trims and lowercases a role name so that {@code "Branch Manager"},
     * {@code "branch manager"} and {@code " BRANCH MANAGER "} are one role.
     *
     * @param role the raw role name, may be null
     * @return the comparable form, or {@code null} when there is nothing to compare
     */
    private static String normalizeRoleName(String role) {
        return role == null ? null : role.trim().toLowerCase();
    }

    /**
     * Returns whether this step is the current decision point on its chain.
     *
     * <p>Used by the service to locate the one step an approver may act on.
     * A step is current when it is the lowest-sequence step still pending.
     *
     * @param steps the full chain, in sequence order
     * @return {@code true} when this step is the active one
     */
    public boolean isCurrentStep(java.util.List<PurchaseRequisitionApproval> steps) {
        Objects.requireNonNull(steps, "The approval chain is required");
        return steps.stream()
                .filter(PurchaseRequisitionApproval::isPending)
                .map(PurchaseRequisitionApproval::getSequenceNo)
                .min(Integer::compareTo)
                .filter(this.sequenceNo::equals)
                .isPresent();
    }

    private static void requireSequence(Integer sequenceNo) {
        if (sequenceNo == null || sequenceNo <= 0) {
            throw new IllegalArgumentException("Approval sequence number must be positive");
        }
    }

    private static BigDecimal scaleMoney(BigDecimal amount) {
        return amount == null
                ? null
                : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
