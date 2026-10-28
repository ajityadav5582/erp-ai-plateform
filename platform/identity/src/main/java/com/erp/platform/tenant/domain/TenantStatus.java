package com.erp.platform.tenant.domain;

/**
 * Tenant status enumeration representing the lifecycle state of a tenant.
 *
 * <p>Each status represents a distinct phase in the tenant lifecycle:
 * <ul>
 *   <li>TRIAL - Tenant is in trial period, has limited access</li>
 *   <li>PENDING - Tenant is newly created, awaiting activation</li>
 *   <li>ACTIVE - Tenant is fully operational and can use the platform</li>
 *   <li>SUSPENDED - Tenant has been temporarily suspended, access blocked</li>
 *   <li>EXPIRED - Tenant subscription has expired, access blocked</li>
 *   <li>DEACTIVATED - Tenant has been permanently deactivated, access blocked</li>
 *   <li>ARCHIVED - Tenant data is archived for compliance, read-only access</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum TenantStatus {

    /**
     * Tenant is in trial period.
     * Access is limited and time-bound.
     */
    TRIAL,

    /**
     * Tenant is newly created and pending activation.
     * Initial setup may be in progress.
     */
    PENDING,

    /**
     * Tenant is active and fully operational.
     * All features according to subscription are available.
     */
    ACTIVE,

    /**
     * Tenant has been temporarily suspended.
     * Access is blocked but can be reactivated.
     */
    SUSPENDED,

    /**
     * Tenant subscription has expired.
     * Access is blocked until renewed.
     */
    EXPIRED,

    /**
     * Tenant has been permanently deactivated.
     * Cannot be reactivated, only archived.
     */
    DEACTIVATED,

    /**
     * Tenant data is archived for compliance.
     * Read-only access for audit purposes.
     */
    ARCHIVED
}
