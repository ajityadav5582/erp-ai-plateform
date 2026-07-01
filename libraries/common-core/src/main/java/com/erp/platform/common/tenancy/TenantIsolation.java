package com.erp.platform.common.tenancy;

/**
 * Utility for tenant isolation in queries.
 *
 * <p>Provides methods for adding tenant filtering to queries
 * and ensuring tenant isolation.
 *
 * @since 1.0.0
 */
public final class TenantIsolation {

    private TenantIsolation() {
        // Utility class
    }

    /**
     * Checks if tenant isolation is enabled.
     *
     * @return true if tenant isolation is enabled
     */
    public static boolean isEnabled() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank();
    }

    /**
     * Gets the current tenant ID.
     *
     * @return the tenant ID
     * @throws IllegalStateException if tenant isolation is not enabled
     */
    public static String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant ID is required but not set");
        }
        return tenantId;
    }

    /**
     * Adds tenant filter to a query.
     *
     * @param query the query to filter
     * @param tenantField the tenant field name
     * @return the filtered query
     */
    public static String addTenantFilter(String query, String tenantField) {
        if (!isEnabled()) {
            return query;
        }
        String tenantId = TenantContext.getTenantId();
        return query + " AND " + tenantField + " = '" + tenantId + "'";
    }
}
