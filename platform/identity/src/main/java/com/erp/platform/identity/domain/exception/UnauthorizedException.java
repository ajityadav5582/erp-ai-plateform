package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a user is not authorized to perform an operation.
 *
 * @since 1.0.0
 */
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }

    /**
     * Creates an UnauthorizedException for insufficient role.
     *
     * @param requiredRole the role that was required
     * @return the exception
     * @since 1.0.0
     */
    public static UnauthorizedException forRole(String requiredRole) {
        return new UnauthorizedException("Required role: " + requiredRole);
    }

    /**
     * Creates an UnauthorizedException for insufficient roles.
     *
     * @param requiredRoles the roles that were required
     * @return the exception
     * @since 1.0.0
     */
    public static UnauthorizedException forAnyRole(String... requiredRoles) {
        return new UnauthorizedException("Required any of roles: " + String.join(", ", requiredRoles));
    }
}
