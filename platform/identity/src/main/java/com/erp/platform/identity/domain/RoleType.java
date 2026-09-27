package com.erp.platform.identity.domain;

/**
 * Role type enumeration.
 *
 * <p>Distinguishes between platform roles, automatically provisioned tenant roles,
 * and tenant-created custom roles.
 *
 * @since 1.0.0
 */
public enum RoleType {

    /**
     * Roles owned by the ERP platform. A system role can be global or have a
     * tenant-scoped instance when the platform provisions it for tenant use.
     */
    SYSTEM,

    /** Roles automatically provisioned within a tenant for standard administration. */
    TENANT,

    /** Roles created and managed by tenant administrators. */
    CUSTOM
}
