package com.erp.platform.common.tenancy;

/**
 * Listener for tenant events.
 *
 * <p>This interface provides a callback for tenant context changes.
 *
 * @since 1.0.0
 */
public interface TenantEventListener {

    /**
     * Called when a tenant event occurs.
     *
     * @param event the tenant event
     */
    void onTenantEvent(TenantEvent event);
}
