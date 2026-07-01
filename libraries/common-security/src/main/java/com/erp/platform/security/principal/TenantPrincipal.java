package com.erp.platform.security.principal;

import java.io.Serializable;

/**
 * Tenant principal for multi-tenant security.
 *
 * <p>Represents the tenant context in security operations.
 * This is used to ensure all security decisions are tenant-aware.
 *
 * @since 1.0.0
 */
public record TenantPrincipal(
    Long tenantId,
    String tenantName,
    String tenantDomain
) implements Serializable {

    /**
     * Creates a tenant principal.
     */
    public static TenantPrincipal of(Long tenantId, String tenantName, String tenantDomain) {
        return new TenantPrincipal(tenantId, tenantName, tenantDomain);
    }

    /**
     * Checks if this tenant matches another tenant.
     *
     * @param other the other tenant
     * @return true if same tenant
     */
    public boolean equals(TenantPrincipal other) {
        if (other == null) {
            return false;
        }
        return this.tenantId != null && this.tenantId.equals(other.tenantId);
    }
}
