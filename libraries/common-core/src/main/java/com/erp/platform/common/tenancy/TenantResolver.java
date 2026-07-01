package com.erp.platform.common.tenancy;

/**
 * Resolves the tenant ID from the current request.
 *
 * <p>This interface provides a strategy for resolving the tenant ID
 * from various sources such as headers, tokens, or subdomains.
 *
 * @since 1.0.0
 */
public interface TenantResolver {

    /**
     * Resolves the tenant ID from the current request.
     *
     * @return the tenant ID
     */
    String resolveTenantId();
}
