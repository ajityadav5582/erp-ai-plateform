package com.erp.platform.common.tenancy;

/**
 * Event fired when tenant context changes.
 *
 * <p>This event is published when the tenant context is set or cleared,
 * allowing other components to react to tenant changes.
 *
 * @since 1.0.0
 */
public record TenantEvent(String tenantId, TenantEventType type) {

    /**
     * Tenant event types.
     */
    public enum TenantEventType {
        /** Tenant context was set */
        SET,
        /** Tenant context was cleared */
        CLEARED
    }

    /**
     * Creates a new tenant set event.
     *
     * @param tenantId the tenant ID
     * @return the tenant event
     */
    public static TenantEvent ofSet(String tenantId) {
        return new TenantEvent(tenantId, TenantEventType.SET);
    }

    /**
     * Creates a new tenant cleared event.
     *
     * @return the tenant event
     */
    public static TenantEvent ofCleared() {
        return new TenantEvent(null, TenantEventType.CLEARED);
    }
}
