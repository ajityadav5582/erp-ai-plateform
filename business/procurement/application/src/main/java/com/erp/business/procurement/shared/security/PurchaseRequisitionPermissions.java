package com.erp.business.procurement.shared.security;

/**
 * The purchase-requisition permission matrix, in one place.
 *
 * <p>Codes follow the platform convention fixed by
 * {@code Permission.generatePermissionCode(resource, action)}:
 * {@code RESOURCE_ACTION}, upper case, with {@code RESOURCE} naming the
 * <em>entity</em> and {@code ACTION} naming the single business operation it
 * unlocks. Resource {@code PR} and action {@code VIEW} give {@code PR_VIEW}.
 * Every other seeded permission obeys that shape - {@code BRANCH_VIEW},
 * {@code ROLE_DEACTIVATE}, {@code INVENTORY_ADJUST}, {@code PRODUCT_CREATE} -
 * and the Role screen groups its checkbox list by exactly that resource value.
 * Naming the codes as literals at each call site is how a permission matrix
 * quietly rots: a typo compiles fine and simply denies everyone.
 *
 * <p><strong>One permission per business operation.</strong> This module used to
 * gate three different actions - raising, editing and submitting a draft - behind
 * a single {@code PURCHASE_REQUISITION_CREATE}, and put approving and rejecting on
 * one {@code PURCHASE_REQUISITION_APPROVE} because they share an endpoint. Both
 * were wrong in the same way: a company that wanted to let a buyer submit a draft
 * it did not author, or let a manager reject spend without letting them approve
 * it, could not be given that without also being given the wider permission.
 * Splitting to one code per operation is what makes the matrix expressible, and it
 * is the whole reason this file was rewritten rather than extended.
 *
 * <p>Requisition permissions therefore sit under resource {@code PR} rather than a
 * module-wide {@code PURCHASE}, which is what lets an administrator grant
 * requisition access without also being handed purchase-order access. The two
 * documents have different owners - a requisition is an internal request for
 * spend, an order is a commitment to a supplier - and a Role screen that cannot
 * express that split cannot enforce it. Purchase orders live in
 * {@link PurchaseOrderPermissions}.
 *
 * <p><strong>Every constant below must exist as a row in Identity's
 * {@code permissions} table</strong>, seeded by
 * {@code DataInitializer.ensureSupportedPermissionsExist()}. This is not a
 * formality: a permission code with no row cannot be granted to any role, so it can
 * never appear in an access token, so {@code PermissionAspect} rejects every caller
 * of the annotated method - for every role, including {@code SUPER_ADMIN}. The
 * failure is silent in both directions. No compilation error, no startup error, no
 * warning, and the resulting 403 names a permission that does not exist anywhere in
 * the system.
 *
 * <p>This happened here. The cancel, delete, order-award and admin codes were
 * declared and used on {@code awardToOrder}, {@code cancel} and {@code delete},
 * while Identity seeded only three of them - so converting an approved requisition
 * into a purchase order was impossible for anyone. Adding a constant to this file
 * therefore carries a matching obligation in the seeder, and the two lists must be
 * kept in step by hand.
 *
 * <p><strong>No roles are defined here.</strong> There are no buyer roles, no
 * approver roles and no hard-coded role-to-permission mapping. These are permission
 * codes only; the pairing of a person to a set of them is made by an administrator
 * on the Role screen, and is stored against the role.
 *
 * <p><strong>What each permission is for.</strong>
 * <ul>
 *   <li>{@link #VIEW} - read requisitions, their lines and their approval chain.
 *       Also required to read the actionability report, because it describes the
 *       document.</li>
 *   <li>{@link #CREATE} - raise a new draft.</li>
 *   <li>{@link #EDIT} - change the content of a draft someone else raised. Held by
 *       buyers and administrative assistants who are not the requester, and by the
 *       requester for their own draft. Deliberately separate from {@link #CREATE}:
 *       creating a request is an ordinary act, editing another person's request is
 *       an intervention.</li>
 *   <li>{@link #SUBMIT} - put a draft into the approval chain. Held by requesters.
 *       Separate from {@link #EDIT} because submitting is the point at which the
 *       document stops belonging to its author and starts being somebody else's
 *       decision.</li>
 *   <li>{@link #APPROVE} - record an approval that lets the spend proceed. Held by
 *       approvers. Necessary but <em>not</em> sufficient:
 *       {@code PermissionAspect} only proves the caller holds the permission, so
 *       the service additionally requires that the caller is the step's actual
 *       assignee. A requester who holds this permission and is the assignee may
 *       approve their own requisition - in a small business there is frequently
 *       nobody else to ask, and a company that does want a second pair of eyes
 *       gets it by pointing the step at someone else.</li>
 *   <li>{@link #REJECT} - record an approval that stops the spend. Deliberately
 *       separate from {@link #APPROVE}: a company frequently wants an approver who
 *       can say no without being able to say yes, and collapsing the two makes
 *       that impossible to grant. It is not the weaker permission - rejecting is
 *       the only decision that is final on a single step - so it carries the same
 *       assignee check as {@link #APPROVE}.</li>
 *   <li>{@link #CANCEL} - withdraw a requisition that has left draft. Distinct
 *       from {@link #REJECT} because cancelling is the <em>requester's</em> right
 *       and rejecting is the <em>approver's</em>; the same document can be stopped
 *       either way, by different people, at different times.</li>
 *   <li>{@link #CONVERT_TO_PO} - turn approved quantity into a purchase order. Held
 *       by buyers. Deliberately separate from {@link #APPROVE}: the people who
 *       approve spend should not also be the people who commit it to a supplier.
 *       Named for what it does rather than {@code AWARD}, so {@code PR_CONVERT_TO_PO}
 *       cannot be misread for a purchase-order permission.</li>
 *   <li>{@link #DELETE} - remove a draft outright.</li>
 *   <li>{@link #ADMIN} - act on <em>another user's</em> requisition. This one exists
 *       solely to bypass the ownership check on edit, cancel and delete, for the
 *       case where someone raises a draft and then leaves. It deliberately does not
 *       bypass approval assignment: an administrator who can clear other people's
 *       documents should not thereby decide approval steps nobody assigned to
 *       them. It says nothing about who may approve - that is {@link #APPROVE} and
 *       {@link #REJECT}, and nothing else grants it.</li>
 * </ul>
 *
 * <p>Permissions are read from the validated access token by the shared
 * {@code PermissionAspect} in {@code libraries:common-security}. No service calls
 * back into Identity to authorize a request, and nothing here is read from a header
 * or body, so a caller cannot widen their own authority.
 *
 * @since 1.0.0
 */
public final class PurchaseRequisitionPermissions {

    /** Resource code. Identity seeds every constant below as {@code PR_<ACTION>}. */
    public static final String RESOURCE = "PR";

    /** Read requisitions, their lines and their approval chain. */
    public static final String VIEW = "PR_VIEW";

    /** Raise a new draft requisition. */
    public static final String CREATE = "PR_CREATE";

    /** Change the content of a draft requisition. */
    public static final String EDIT = "PR_EDIT";

    /** Put a draft into the approval chain. */
    public static final String SUBMIT = "PR_SUBMIT";

    /** Record an approval that lets the spend proceed. */
    public static final String APPROVE = "PR_APPROVE";

    /** Record an approval that stops the spend. */
    public static final String REJECT = "PR_REJECT";

    /** Withdraw a requisition. */
    public static final String CANCEL = "PR_CANCEL";

    /** Convert approved requisition quantity into a purchase order. */
    public static final String CONVERT_TO_PO = "PR_CONVERT_TO_PO";

    /** Remove a draft requisition outright. */
    public static final String DELETE = "PR_DELETE";

    /** Act on another user's requisition. Bypasses ownership only. */
    public static final String ADMIN = "PR_ADMIN";

    private PurchaseRequisitionPermissions() {
        throw new AssertionError("No instances");
    }
}
