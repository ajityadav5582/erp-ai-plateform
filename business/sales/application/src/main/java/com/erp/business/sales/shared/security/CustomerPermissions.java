package com.erp.business.sales.shared.security;

/**
 * Permissions for the customer master.
 *
 * <p>Customer master data is a shared resource across the whole sell side, so it
 * is a resource of its own rather than an action on "sales". Every code here must
 * have a matching row in Identity's {@code permissions} table, seeded by
 * {@code DataInitializer}. A code with no row can never be granted to a role, can
 * never appear in an access token, and so closes the annotated method to every
 * caller including {@code SUPER_ADMIN}.
 *
 * @since 1.0.0
 */
public final class CustomerPermissions {

    /** Read customers. */
    public static final String CUSTOMER_VIEW = "CUSTOMER_VIEW";

    /** Create a customer. */
    public static final String CUSTOMER_CREATE = "CUSTOMER_CREATE";

    /** Edit a customer's details. */
    public static final String CUSTOMER_UPDATE = "CUSTOMER_UPDATE";

    /** Delete a customer. */
    public static final String CUSTOMER_DELETE = "CUSTOMER_DELETE";

    private CustomerPermissions() {
        throw new AssertionError("No instances");
    }
}
