package com.erp.platform.tenant.domain;

/**
 * Isolation strategy enumeration defining how tenant data is isolated.
 *
 * <p>Each strategy provides different levels of data isolation:
 * <ul>
 *   <li>SHARED_SCHEMA - All tenants share the same database schema with row-level security</li>
 *   <li>SCHEMA_PER_TENANT - Each tenant has its own schema within a shared database</li>
 *   <li>DATABASE_PER_TENANT - Each tenant has its own dedicated database</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum IsolationStrategy {

    /**
     * All tenants share the same database schema.
     * Data isolation is achieved through row-level security and tenant_id filtering.
     * Most cost-effective for multi-tenant SaaS platforms.
     */
    SHARED_SCHEMA,

    /**
     * Each tenant has its own database schema.
     * Provides better isolation than shared schema while sharing database resources.
     * Good balance between isolation and resource efficiency.
     */
    SCHEMA_PER_TENANT,

    /**
     * Each tenant has its own dedicated database.
     * Provides the highest level of isolation and security.
     * Most resource-intensive but best for high-security requirements.
     */
    DATABASE_PER_TENANT
}
