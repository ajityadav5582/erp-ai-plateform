package com.erp.business.procurement.workflow.domain;

import com.erp.platform.data.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Company-level switches describing how much of the approval workflow a company
 * runs at all.
 *
 * <p><strong>This is not part of the requisition aggregate.</strong> It is one row
 * per company, read by every requisition in that company and never cascaded from
 * one. The same reasoning as {@code PurchaseRequisitionApprovalPolicy}: a document
 * must never be able to rewrite the rules that govern it.
 *
 * <p><strong>Why this is a switch and not a policy row.</strong> An approval policy
 * answers <em>who</em> must sign off. It cannot answer <em>whether</em> anyone does,
 * because the chain resolver assumes at least one step has to exist - a policy with
 * no rows was read as "not configured yet", and the module fell back to a single
 * permission-addressed step. A company of one or two people therefore had no way to
 * say "nobody signs off; the owner is the approver", and every requisition sat in a
 * queue its only possible approver had to clear by hand.
 *
 * <p>{@link #purchaseRequisitionApprovalRequired} is that missing answer:
 *
 * <pre>
 *   true  (default) - DRAFT -> SUBMITTED -> APPROVED, through the approval chain
 *   false           - DRAFT -> SUBMITTED -> APPROVED, in one act at submission
 * </pre>
 *
 * <p><strong>The default is {@code true}, and that is the safe direction.</strong> A
 * company with no row configured is treated as requiring approval. Defaulting the
 * other way would mean every company that had never been configured - which is all
 * of them, on day one - silently had its spend control switched off the first time
 * somebody submitted a requisition. Turning the control off has to be a decision
 * somebody makes on the record.
 *
 * <p><strong>Turning it off is not the same as bypassing permissions.</strong> It
 * changes <em>when</em> the document is approved, never <em>who may</em> raise or
 * submit it. Submission still requires {@code PR_SUBMIT} from the token and still
 * requires the caller to be the requester; the auto-approval is performed by the
 * service on the submitter's behalf and is recorded as such in the document's
 * audit trail.
 *
 * <p><strong>One row per company.</strong> Enforced by a unique constraint rather
 * than only by the repository method name, so a concurrent double-create cannot
 * leave two contradictory settings behind with whichever one happened to be read
 * first deciding the workflow.
 *
 * @since 1.0.0
 */
@Entity
@Table(
        name = "company_workflow_configurations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_company_workflow_configurations_tenant_company",
                columnNames = {"tenant_id", "company_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CompanyWorkflowConfiguration extends TenantScopedEntity<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * Whether a requisition must be approved by a person before it may be
     * converted into a purchase order.
     *
     * <p>{@code true} routes the document through the approval chain resolved from
     * the effective policy. {@code false} approves it as part of submission, and the
     * audit trail records the approval as having been automatic so that a later
     * reader can tell an auto-approval from a decision a person made.
     */
    @Column(name = "purchase_requisition_approval_required", nullable = false)
    @Builder.Default
    private boolean purchaseRequisitionApprovalRequired = true;

    /**
     * Creates a configuration for a company with an explicit setting.
     *
     * <p>There is no "create with the default" overload. The default is {@code
     * true}, and a caller reaching for a two-argument constructor is making a
     * decision that should be written down at the call site rather than inferred
     * from a field initialiser - a company that wants approval switched off should
     * say so where the reader of the code can see it.
     *
     * @param tenantId                      the owning tenant
     * @param companyId                     the owning company
     * @param purchaseRequisitionApprovalRequired whether approval is required
     * @return a new unsaved configuration carrying that setting
     */
    public static CompanyWorkflowConfiguration create(
            Long tenantId, Long companyId, boolean purchaseRequisitionApprovalRequired) {

        CompanyWorkflowConfiguration configuration = CompanyWorkflowConfiguration.builder()
                .purchaseRequisitionApprovalRequired(purchaseRequisitionApprovalRequired)
                .build();
        configuration.assignScope(tenantId, companyId);
        return configuration;
    }

    /**
     * Turns the requisition approval requirement on or off.
     *
     * @param required {@code true} to route requisitions through the approval
     *                 chain, {@code false} to approve them at submission
     */
    public void requirePurchaseRequisitionApproval(boolean required) {
        this.purchaseRequisitionApprovalRequired = required;
    }

    /**
     * Returns whether a requisition must be approved by a person.
     *
     * @return {@code true} when the approval chain applies
     */
    public boolean isPurchaseRequisitionApprovalRequired() {
        return purchaseRequisitionApprovalRequired;
    }
}
