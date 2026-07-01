package com.erp.platform.common.tenancy;

/**
 * Context for managing tenant information in a multi-tenant application.
 *
 * <p>This class provides thread-local storage for the current tenant ID,
 * enabling tenant isolation across the application.
 *
 * @since 1.0.0
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Sets the current tenant ID.
     *
     * @param tenantId the tenant ID
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID.
     *
     * @return the tenant ID, or null if not set
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clears the current tenant ID.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
