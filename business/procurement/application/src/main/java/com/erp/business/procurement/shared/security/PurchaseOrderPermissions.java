package com.erp.business.procurement.shared.security;

/**
 * The purchase-order permission matrix, in one place.
 *
 * <p>Separate from {@link PurchaseRequisitionPermissions} because requisitions and purchase
 * orders are different documents with different owners. A requisition is an internal
 * request for spend; a purchase order is a commitment to a supplier. The person who
 * raises a requisition is normally not the person who signs the order, and neither
 * is the person who receives the goods. Keeping the two resources separate is what
 * lets an administrator express that split in the Role screen instead of collapsing
 * it into one "can buy things" permission.
 *
 * <p>Codes follow the platform convention fixed by
 * {@code Permission.generatePermissionCode(resource, action)}:
 * {@code RESOURCE_ACTION}, upper case, resource {@code PURCHASE_ORDER}. Each one is
 * a row in Identity's {@code permissions} table, seeded by
 * {@code DataInitializer.ensureSupportedPermissionsExist()} - without that row the
 * code can never be granted, so it can never appear in a token, so the operation is
 * closed to every role including {@code SUPER_ADMIN}.
 *
 * <p><strong>Status.</strong> The actions are derived from the lifecycle the
 * {@code PurchaseOrderStatus} aggregate already enforces:
 * {@code DRAFT -> SUBMITTED -> APPROVED -> PARTIALLY_RECEIVED -> RECEIVED -> CLOSED},
 * with {@code REJECTED} and {@code CANCELLED} as the off-ramps. These permissions
 * are declared and seeded ahead of the endpoints that enforce them: the order
 * aggregate, its lines and its repository all exist, but there is no purchase-order
 * REST layer yet. Until that lands these rows are selectable in the Role screen but
 * gate nothing - which is the intended order of work, since an administrator can
 * build the roles first and the endpoints inherit them when they arrive.
 *
 * @since 1.0.0
 */
public final class PurchaseOrderPermissions {

    /** Read purchase orders and their receipt progress. */
    public static final String VIEW = "PURCHASE_ORDER_VIEW";

    /** Raise a draft order from approved requisition quantity. Held by buyers. */
    public static final String CREATE = "PURCHASE_ORDER_CREATE";

    /**
     * Approve or reject a submitted order.
     *
     * <p>Deliberately not the same permission as {@link #CREATE}: the people who
     * commit spend to a supplier should not be the people who sign it off.
     */
    public static final String APPROVE = "PURCHASE_ORDER_APPROVE";

    /**
     * Record goods received against a line.
     *
     * <p>Separate again, because receipt is a warehouse action. It moves inventory
     * and can only ever increase {@code receivedAmount}, so it belongs to whoever
     * physically receives the delivery, not to the approver or the buyer.
     */
    public static final String RECEIVE = "PURCHASE_ORDER_RECEIVE";

    /** Void an order before or during receipt. */
    public static final String CANCEL = "PURCHASE_ORDER_CANCEL";

    /** Remove a draft order outright. */
    public static final String DELETE = "PURCHASE_ORDER_DELETE";

    private PurchaseOrderPermissions() {
        throw new AssertionError("No instances");
    }
}
