package com.erp.platform.identity.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request carries no tenant context.
 *
 * <p>Reports {@code 403 MISSING_TENANT_CONTEXT}.
 *
 * @since 1.0.0
 */
public class MissingTenantContextException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public MissingTenantContextException() {
        super(HttpStatus.FORBIDDEN, "MISSING_TENANT_CONTEXT",
                "Tenant context is missing for the current request");
    }

    public MissingTenantContextException(String message) {
        super(HttpStatus.FORBIDDEN, "MISSING_TENANT_CONTEXT", message);
    }
}
