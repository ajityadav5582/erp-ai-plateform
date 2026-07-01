package com.erp.platform.common.tenancy;

/**
 * Filter for tenant isolation in repositories.
 *
 * <p>This interface provides a strategy for filtering entities
 * by tenant ID.
 *
 * @param <T> the entity type
 * @since 1.0.0
 */
public interface TenantFilter<T> {

    /**
     * Applies tenant filtering to the query.
     *
     * @param query the query to filter
     * @return the filtered query
     */
    String apply(String query);

    /**
     * Gets the tenant field name.
     *
     * @return the tenant field name
     */
    String getTenantField();
}
