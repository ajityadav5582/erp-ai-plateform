package com.erp.platform.business.context;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.context.BusinessContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Shared business context accessor used by all business modules (procurement,
 * sales, pos, inventory, ...).
 *
 * <p>This is the single canonical implementation. Business modules must depend on
 * {@code :libraries:common-business-context} and inject this class instead of
 * defining their own copy. Keeping one implementation guarantees that tenant,
 * company, fiscal-year and user resolution behave identically across modules.
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

    public String getCurrentUserFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user.getFullName();
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
        if (role == null || role.isBlank()) {
            return false;
        }
        return getCurrentUserRoles().contains(role);
    }

    public boolean isAnyAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN")
                || roles.contains("TENANT_ADMIN")
                || roles.contains("OWNER")
                || roles.contains("ADMIN");
    }
}
