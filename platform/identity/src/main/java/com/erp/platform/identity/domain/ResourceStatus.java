package com.erp.platform.identity.domain;

/**
 * Status of a business resource (module) definition.
 *
 * <p>Resources are master data shared across tenants and are used to
 * categorize permissions and organize the ERP platform modules.
 *
 * @since 1.0.0
 */
public enum ResourceStatus {

    /**
     * Resource is available and usable.
     */
    ACTIVE,

    /**
     * Resource is temporarily unavailable.
     */
    INACTIVE
}
