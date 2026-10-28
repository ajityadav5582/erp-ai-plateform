package com.erp.platform.security.aspect;

import com.erp.platform.security.annotation.RequiresPermission;
import com.erp.platform.security.annotation.RequiresRole;
import com.erp.platform.security.context.SecurityContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Enforces declarative authorization annotations at the service layer.
 *
 * <p>Permissions and roles come from the validated access-token claims, so no
 * service needs to call back into Identity to authorize a request.
 *
 * @since 1.0.0
 */
@Aspect
@Component
public class PermissionAspect {

    private final SecurityContext securityContext;

    public PermissionAspect(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        if (!securityContext.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required for " + joinPoint.getSignature().toShortString());
        }

        String[] expressions = requiresPermission.value();
        boolean granted = requiresPermission.logical() == RequiresPermission.LogicalOperator.AND
                ? securityContext.hasAllPermissions(expressions)
                : securityContext.hasAnyPermission(expressions);

        if (!granted) {
            throw new AccessDeniedException(
                    "Missing required permission(s): " + String.join(", ", expressions));
        }
    }

    @Before("@annotation(requiresRole)")
    public void checkRole(JoinPoint joinPoint, RequiresRole requiresRole) {
        if (!securityContext.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication required for " + joinPoint.getSignature().toShortString());
        }

        // @RequiresRole has no logical operator; holding any of the roles grants access.
        String[] roles = requiresRole.value();
        boolean granted = securityContext.hasAnyRole(roles);

        if (!granted) {
            throw new AccessDeniedException(
                    "Missing required role(s): " + String.join(", ", roles));
        }
    }
}
