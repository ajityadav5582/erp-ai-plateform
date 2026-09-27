package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when the tenant context is missing or cannot be resolved
 * for the current authenticated request.
 *
 * <p>This is a security-level exception that maps to a 403 Forbidden response,
 * indicating that the authenticated user does not have an associated tenant
 * context required to proceed.
 *
 * @since 1.0.0
 */
public class MissingTenantContextException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public MissingTenantContextException() {
        super("Tenant context is missing for the current request");
    }

    public MissingTenantContextException(String message) {
        super(message);
    }
}
