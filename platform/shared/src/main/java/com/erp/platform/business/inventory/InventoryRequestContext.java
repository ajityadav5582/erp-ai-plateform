package com.erp.platform.business.inventory;

import java.util.Map;

/**
 * Thread-local holder for Inventory-bound headers captured on the request thread
 * so async workers (e.g. goods receipt stock post) can forward them. Shared
 * across all business modules that call Inventory.
 */
public final class InventoryRequestContext {

    private static final ThreadLocal<Map<String, String>> HEADERS = new ThreadLocal<>();

    private InventoryRequestContext() {
    }

    public static void set(Map<String, String> headers) {
        HEADERS.set(headers);
    }

    public static Map<String, String> get() {
        return HEADERS.get();
    }

    public static void clear() {
        HEADERS.remove();
    }
}
