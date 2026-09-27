package com.erp.platform.identity.application.security;

import com.erp.platform.identity.application.AuthorizationService;
import com.erp.platform.identity.domain.exception.AccessDeniedException;
import com.erp.platform.security.annotation.RequiresPermission;
import com.erp.platform.security.authorization.HasPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP aspect that enforces permission-based access control.
 *
 * <p>Intercepts methods annotated with {@link RequirePermission}, {@link RequiresPermission},
 * or {@link HasPermission} and verifies that the authenticated user has the required permission(s).
 *
 * <p>SUPER_ADMIN users bypass all permission checks.
 *
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final AuthorizationService authorizationService;

    /**
     * Intercepts method calls annotated with {@link RequirePermission}.
     *
     * @param joinPoint the join point
     * @param requirePermission the annotation
     * @throws AccessDeniedException if the user lacks the required permission
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        if (authorizationService.isAnyAdmin()) {
            return;
        }

        String[] requiredPermissions = requirePermission.value();
        boolean requireAll = requirePermission.requireAll();

        if (requiredPermissions.length == 1) {
            if (!authorizationService.hasPermission(requiredPermissions[0])) {
                throw AccessDeniedException.forPermission(requiredPermissions[0]);
            }
        } else if (requireAll) {
            if (!authorizationService.hasAllPermissions(requiredPermissions)) {
                throw AccessDeniedException.forAllPermissions(requiredPermissions);
            }
        } else {
            if (!authorizationService.hasAnyPermission(requiredPermissions)) {
                throw AccessDeniedException.forAnyPermission(requiredPermissions);
            }
        }

        log.debug("Permission check passed for method: {}", getMethodName(joinPoint));
    }

    /**
     * Intercepts type-level {@link RequirePermission} annotations.
     *
     * @param joinPoint the join point
     * @param requirePermission the annotation
     * @throws AccessDeniedException if the user lacks the required permission
     */
    @Before("@within(requirePermission)")
    public void checkTypePermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getAnnotation(RequirePermission.class) != null) {
            return;
        }

        checkPermission(joinPoint, requirePermission);
    }

    /**
     * Intercepts method calls annotated with {@link RequiresPermission}.
     *
     * @param joinPoint the join point
     * @param requiresPermission the annotation
     */
    @Before("@annotation(requiresPermission)")
    public void checkRequiresPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        if (authorizationService.isAnyAdmin()) {
            return;
        }

        String[] requiredPermissions = requiresPermission.value();
        boolean requireAll = requiresPermission.logical() == RequiresPermission.LogicalOperator.AND;

        if (requiredPermissions.length == 1) {
            if (!authorizationService.hasPermission(requiredPermissions[0])) {
                throw AccessDeniedException.forPermission(requiredPermissions[0]);
            }
        } else if (requireAll) {
            if (!authorizationService.hasAllPermissions(requiredPermissions)) {
                throw AccessDeniedException.forAllPermissions(requiredPermissions);
            }
        } else {
            if (!authorizationService.hasAnyPermission(requiredPermissions)) {
                throw AccessDeniedException.forAnyPermission(requiredPermissions);
            }
        }

        log.debug("RequiresPermission check passed for method: {}", getMethodName(joinPoint));
    }

    /**
     * Intercepts method calls annotated with {@link HasPermission}.
     *
     * @param joinPoint the join point
     * @param hasPermission the annotation
     */
    @Before("@annotation(hasPermission)")
    public void checkHasPermission(JoinPoint joinPoint, HasPermission hasPermission) {
        if (authorizationService.isAnyAdmin()) {
            return;
        }

        String permission = hasPermission.value();
        if (!authorizationService.hasPermission(permission)) {
            throw AccessDeniedException.forPermission(permission);
        }

        log.debug("HasPermission check passed for method: {}", getMethodName(joinPoint));
    }

    private String getMethodName(JoinPoint joinPoint) {
        if (joinPoint == null || joinPoint.getSignature() == null) {
            return "unknown";
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> declaringType = signature.getDeclaringType();
        String className = declaringType != null ? declaringType.getSimpleName() : "Unknown";
        String methodName = signature.getMethod() != null ? signature.getMethod().getName() : signature.getName();
        return className + "." + methodName;
    }
}
