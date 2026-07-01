package com.erp.platform.common.tenancy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publishes tenant events to registered listeners.
 *
 * <p>This class manages tenant event listeners and publishes
 * events when the tenant context changes.
 *
 * @since 1.0.0
 */
public class TenantEventPublisher {

    private final List<TenantEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a tenant event listener.
     *
     * @param listener the listener to register
     */
    public void register(TenantEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters a tenant event listener.
     *
     * @param listener the listener to unregister
     */
    public void unregister(TenantEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publishes a tenant event to all registered listeners.
     *
     * @param event the tenant event
     */
    public void publish(TenantEvent event) {
        for (TenantEventListener listener : listeners) {
            listener.onTenantEvent(event);
        }
    }
}
