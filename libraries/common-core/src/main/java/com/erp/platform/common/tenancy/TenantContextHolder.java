package com.erp.platform.common.tenancy;

/**
 * Holds tenant context across the application.
 *
 * <p>This class provides a centralized place to manage tenant context,
 * including setting, getting, and clearing the current tenant.
 *
 * @since 1.0.0
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();
    private static final TenantEventPublisher EVENT_PUBLISHER = new TenantEventPublisher();

    private TenantContextHolder() {
        // Utility class
    }

    /**
     * Sets the current tenant ID.
     *
     * @param tenantId the tenant ID
     */
    public static void setTenantId(String tenantId) {
        String previousTenantId = CONTEXT.get();
        CONTEXT.set(tenantId);
        EVENT_PUBLISHER.publish(TenantEvent.ofSet(tenantId));
    }

    /**
     * Gets the current tenant ID.
     *
     * @return the tenant ID, or null if not set
     */
    public static String getTenantId() {
        return CONTEXT.get();
    }

    /**
     * Clears the current tenant ID.
     */
    public static void clear() {
        String previousTenantId = CONTEXT.get();
        CONTEXT.remove();
        EVENT_PUBLISHER.publish(TenantEvent.ofCleared());
    }

    /**
     * Checks if a tenant is currently set.
     *
     * @return true if a tenant is set
     */
    public static boolean hasTenant() {
        return CONTEXT.get() != null;
    }

    /**
     * Registers a tenant event listener.
     *
     * @param listener the listener to register
     */
    public static void registerListener(TenantEventListener listener) {
        EVENT_PUBLISHER.register(listener);
    }

    /**
     * Unregisters a tenant event listener.
     *
     * @param listener the listener to unregister
     */
    public static void unregisterListener(TenantEventListener listener) {
        EVENT_PUBLISHER.unregister(listener);
    }
}
