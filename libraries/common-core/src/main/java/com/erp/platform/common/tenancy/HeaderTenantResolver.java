package com.erp.platform.common.tenancy;

/**
 * Resolves tenant ID from HTTP headers.
 *
 * <p>This resolver extracts the tenant ID from a configurable
 * HTTP header.
 *
 * @since 1.0.0
 */
public class HeaderTenantResolver implements TenantResolver {

    private final String headerName;

    /**
     * Creates a new header tenant resolver.
     *
     * @param headerName the HTTP header name containing the tenant ID
     */
    public HeaderTenantResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String resolveTenantId() {
        // In a real implementation, this would access the HTTP request
        // For now, return from thread-local context
        return TenantContext.getTenantId();
    }
}
