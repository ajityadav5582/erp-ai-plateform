package com.erp.platform.common.tenancy;

/**
 * Context for managing branch information in a multi-tenant application.
 *
 * <p>This class provides thread-local storage for the current branch ID,
 * enabling branch-scoped operations across the application. The branch is
 * the third level of scoping after tenant and company: a user operates on
 * one or more branches within the active company, and every write operation
 * is attributed to the branch selected via the {@code X-Branch-Id} header.
 *
 * <p>The value is trusted because it is only ever populated from the header
 * by the {@code BranchFilter}, never from a request body or query parameter.
 * Callers must still combine it with the tenant id and company id, so a
 * header pointing at another tenant's or company's branch cannot be used
 * to read or mutate that branch's data.
 *
 * @since 1.0.0
 */
public final class BranchContext {

    private static final ThreadLocal<Long> CURRENT_BRANCH = new ThreadLocal<>();

    private BranchContext() {
        // Utility class
    }

    /**
     * Sets the current branch ID.
     *
     * @param branchId the branch ID
     */
    public static void setBranchId(Long branchId) {
        CURRENT_BRANCH.set(branchId);
    }

    /**
     * Gets the current branch ID.
     *
     * @return the branch ID, or null if not set
     */
    public static Long getBranchId() {
        return CURRENT_BRANCH.get();
    }

    /**
     * Clears the current branch ID.
     */
    public static void clear() {
        CURRENT_BRANCH.remove();
    }
}