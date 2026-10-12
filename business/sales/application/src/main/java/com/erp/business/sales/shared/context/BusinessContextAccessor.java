package com.erp.business.sales.shared.context;

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
 * context populated by the shared JWT filter.
 *
 * <p>Every sales query is scoped through here so a caller cannot widen its own
 * scope through the payload: the tenant comes from the signed token claim and the
 * company from the {@code X-Company-Id} header, neither of which a client can set
 * from a JSON body.
 */
@Component
public class BusinessContextAccessor {

    /**
     * Tenant comes from the signed token claim; the client cannot influence it.
     */
    public Long getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "Tenant context not available: request is missing a valid access token");
        }
        return Long.parseLong(tenantId);
    }

    /**
     * Company comes from the X-Company-Id header published on the request by
     * the shared JWT filter. A request without one is rejected rather than defaulted,
     * because defaulting would silently write into an arbitrary company.
     */
    public Long getCompanyId() {
        return BusinessContext.requireCompanyId();
    }

    /**
     * Optional company, for endpoints where company genuinely does not apply.
     */
    public Long findCompanyId() {
        return BusinessContext.getCompanyId();
    }

    public Long getFiscalYearId() {
        return BusinessContext.requireFiscalYearId();
    }

    /**
     * The active fiscal year, or {@code null} when the caller has not selected one.
     */
    public Long findFiscalYearId() {
        return BusinessContext.getFiscalYearId();
    }

    /**
     * The acting user id, used to stamp documents and audit trails.
     */
    public String getUserId() {
        return RequestContext.getUserId();
    }

    /**
     * The effective role names of the acting user, as carried by the validated
     * access token.
     */
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

    /**
     * Returns whether the acting user holds the given permission. A secondary,
     * in-body check for conditional cases; the primary gate remains
     * {@code @RequiresPermission} on the method.
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.hasPermission(permission);
    }

    /**
     * Returns whether the acting user holds the given role.
     */
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Returns whether the acting user holds any administrative role.
     */
    public boolean isAnyAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN")
                || roles.contains("TENANT_ADMIN")
                || roles.contains("OWNER")
                || roles.contains("ADMIN");
    }
}
