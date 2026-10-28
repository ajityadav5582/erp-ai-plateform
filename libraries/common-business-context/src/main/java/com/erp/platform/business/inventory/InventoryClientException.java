package com.erp.platform.business.inventory;

/**
 * Thrown when a call to the Inventory service fails (unreachable, timeout, or
 * no configured adapter). Shared across all business modules that depend on
 * Inventory (procurement, sales, pos).
 */
public class InventoryClientException extends RuntimeException {
    public InventoryClientException(String message) {
        super(message);
    }

    public InventoryClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
