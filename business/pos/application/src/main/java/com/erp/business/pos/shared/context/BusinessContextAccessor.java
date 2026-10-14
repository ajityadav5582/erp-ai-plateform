package com.erp.business.pos.shared.context;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.context.BusinessContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Reads the authenticated tenant, company, fiscal year and user off the request
 * context populated by the shared JWT filter, so a caller cannot widen its own
 * scope through the payload.
 */
@Component
public class BusinessContextAccessor {

    public Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "Tenant context not available: request is missing a valid access token");
        }
        return Long.parseLong(tenantId);
    }

    public Long getCompanyId() {
        return BusinessContext.requireCompanyId();
    }

    public Long findCompanyId() {
        return BusinessContext.getCompanyId();
    }

    public Long getFiscalYearId() {
        return BusinessContext.requireFiscalYearId();
    }

    public Long findFiscalYearId() {
        return BusinessContext.getFiscalYearId();
    }

    public String getUserId() {
        return RequestContext.getUserId();
    }

    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Set.of();
        }
        var roles = user.getRoles();
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.hasPermission(permission);
    }

    public boolean hasRole(String role) {
        return role != null && !role.isBlank() && getCurrentUserRoles().contains(role);
    }

    public boolean isAnyAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN")
                || roles.contains("TENANT_ADMIN")
                || roles.contains("OWNER")
                || roles.contains("ADMIN");
    }

    /**
     * Display name of the acting user, used to snapshot the cashier on a session.
     * Falls back to the username, then the user id, when no full name is present.
     */
    public String getCurrentUserFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                return user.getFullName();
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                return user.getUsername();
            }
        }
        return getUserId();
    }
}
