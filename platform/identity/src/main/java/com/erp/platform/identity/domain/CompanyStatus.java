package com.erp.platform.identity.domain;

/**
 * Company status enumeration.
 *
 * <p>Represents the lifecycle states of a Company within a tenant organization.
 * A Company is a distinct legal/business entity owned by a Tenant. A single
 * Tenant can own multiple Companies (e.g. different brands, subsidiaries, or
 * trading names).
 *
 * @since 1.0.0
 */
public enum CompanyStatus {

    /**
     * Company is active and operational.
     * Users can be assigned to active companies and business records
     * can be created against them.
     */
    ACTIVE,

    /**
     * Company is inactive and not operational.
     * No new assignments should be made to inactive companies.
     * Existing assignments are preserved for audit purposes.
     */
    INACTIVE

}
