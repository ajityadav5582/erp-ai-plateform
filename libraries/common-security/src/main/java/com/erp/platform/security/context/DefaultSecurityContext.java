package com.erp.platform.security.context;

import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.principal.TenantPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link SecurityContext} interface.
 *
 * <p>Provides tenant-aware security information backed by Spring Security's
 * {@link SecurityContextHolder}.
 *
 * @since 1.0.0
 */
@Component
public class DefaultSecurityContext implements SecurityContext {

    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    @Override
    public TenantPrincipal getTenantPrincipal() {
        AuthenticatedUser user = getCurrentUser();
        if (user != null && user.getTenantId() != null) {
            return TenantPrincipal.of(user.getTenantId(), null, null);
        }
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && getCurrentUser() != null;
    }

    @Override
    public boolean hasPermission(String permission) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.hasPermission(permission);
    }

    @Override
    public boolean hasAnyPermission(String... permissions) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (user.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(String... permissions) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null || permissions == null) {
            return false;
        }
        return user.hasAllPermissions(permissions);
    }

    @Override
    public boolean hasRole(String role) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.hasAnyRole(roles);
    }
}
