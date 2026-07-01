package com.erp.platform.common.context;

import org.springframework.util.Assert;

/**
 * Thread-local tenant context holder.
 *
 * <p>Provides access to the current tenant ID throughout the request lifecycle.
 * The tenant context is automatically set by the tenant interceptor and
 * cleared after request completion.
 *
 * <p>This class is designed to be used within the current thread only.
 * It should not be passed between threads.
 *
 * @since 1.0.0
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Sets the current tenant ID.
     *
     * @param tenantId the tenant ID
     * @throws IllegalArgumentException if tenantId is null
     */
    public static void setTenantId(Long tenantId) {
        Assert.notNull(tenantId, "Tenant ID cannot be null");
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Returns the current tenant ID.
     *
     * @return the tenant ID
     * @throws IllegalStateException if no tenant context is set
     */
    public static Long getTenantId() {
        Long tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set. " +
                "Ensure TenantContextFilter is configured.");
        }
        return tenantId;
    }

    /**
     * Returns the current tenant ID, or null if not set.
     *
     * @return the tenant ID, or null
     */
    public static Long getTenantIdOrNull() {
        return CURRENT_TENANT.get();
    }

    /**
     * Checks if a tenant context is set.
     *
     * @return true if tenant context is set, false otherwise
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Clears the current tenant context.
     *
     * <p>This should be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Executes the given runnable with the specified tenant context.
     *
     * @param tenantId the tenant ID to set
     * @param runnable the runnable to execute
     */
    public static void runWithTenant(Long tenantId, Runnable runnable) {
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * Executes the given supplier with the specified tenant context.
     *
     * @param tenantId the tenant ID to set
     * @param supplier the supplier to execute
     * @param <T> the type of the result
     * @return the result of the supplier
     */
    public static <T> T supplyWithTenant(Long tenantId, java.util.function.Supplier<T> supplier) {
        try {
            setTenantId(tenantId);
            return supplier.get();
        } finally {
            clear();
        }
    }
}
