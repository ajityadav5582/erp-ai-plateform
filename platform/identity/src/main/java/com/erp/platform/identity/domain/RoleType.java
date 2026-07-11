package com.erp.platform.identity.domain;

/**
 * Role type enumeration.
 *
 * <p>Distinguishes between system-defined roles and tenant-custom roles.
 *
 * @since 1.0.0
 */
public enum RoleType {

    /**
     * System-defined roles that are shared across all tenants.
     * These roles are predefined, cannot be modified or deleted,
     * and typically include roles like SUPER_ADMIN, TENANT_ADMIN.
     */
    SYSTEM,

    /**
     * Tenant-custom roles that are defined by tenant administrators.
     * These roles can be created, modified, and deleted within the tenant.
     */
    CUSTOM
}
