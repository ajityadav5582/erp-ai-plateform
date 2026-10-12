package com.erp.business.procurement.shared.context;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.security.authentication.AuthenticatedUser;
import com.erp.platform.security.context.BusinessContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

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
     *
     * <p>Used where a period is genuinely part of the document but a request that
     * omits it should still be recorded rather than refused. A purchase requisition
     * is real business data: refusing to raise one because the browser has not sent
     * an {@code X-Fiscal-Year-Id} header would lose the demand, whereas storing it
     * unassigned leaves it visible to the requester and reconcilable later. The
     * caller is expected to log when this returns null, because an unassigned
     * document is invisible to fiscal-year budget reporting.
     *
     * @return the active fiscal year id, or null
     */
    public Long findFiscalYearId() {
        return BusinessContext.getFiscalYearId();
    }

    /**
     * The acting user id, used to stamp workflows and check approval assignment.
     */
    public String getUserId() {
        return RequestContext.getUserId();
    }

    /**
     * The effective role names of the acting user, as carried by the validated
     * access token.
     *
     * <p>Role membership is not held in this service's database, so anything that
     * has to decide whether a caller holds a role - most importantly the
     * requisition approval chain, whose steps may be assigned to a role rather
     * than to a named person - has to read them from here. The values come from
     * claims the identity service signed, never from a request header or body, so
     * a caller cannot widen their own authority.
     *
     * @return the role names, or an empty set when the caller is anonymous or the
     *         token carried none
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
     * Returns whether the acting user holds the given permission.
     *
     * <p>This exists for the handful of checks that cannot be expressed as a plain
     * {@code @RequiresPermission} on the method - chiefly the ownership bypass that
     * lets an administrator act on someone else's requisition. It reads the same
     * token-backed {@link AuthenticatedUser} that
     * {@code PermissionAspect} reads, so an endpoint is not silently mixing two
     * different notions of authority.
     *
     * <p>Note the asymmetry, which is deliberate: {@code @RequiresPermission} is the
     * primary gate on every public service method and is enforced before the method
     * body runs. This is a secondary, in-body check for the conditional case, never
     * a replacement.
     *
     * @param permission the permission code, e.g. {@code PURCHASE_ADMIN}
     * @return {@code true} when the caller holds it
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
     *
     * @param role the role name, e.g. "TENANT_ADMIN", "SUPER_ADMIN", "OWNER"
     * @return true when the caller holds it
     */
    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Returns whether the acting user holds any administrative role (SUPER_ADMIN, TENANT_ADMIN, OWNER, ADMIN).
     *
     * @return true when the caller holds an administrative role
     */
    public boolean isAnyAdmin() {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains("SUPER_ADMIN")
                || roles.contains("TENANT_ADMIN")
                || roles.contains("OWNER")
                || roles.contains("ADMIN");
    }
}
