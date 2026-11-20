package com.erp.platform.identity.application;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.identity.domain.exception.UnauthorizedBranchAccessException;
import com.erp.platform.identity.infrastructure.persistence.UserBranchRepository;
import com.erp.platform.security.authentication.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Evaluates whether the current user has access to a given branch within the
 * active tenant and company scope.
 *
 * <p>Access is granted when:
 * <ul>
 *   <li>The user has a {@code user_branches} row linking them to the branch
 *       within the same tenant and company</li>
 *   <li>The user holds {@code COMPANY_ADMIN} or {@code BRANCH_ALL_ACCESS}
 *       permission for the active company</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BranchAccessEvaluator {

    private final UserBranchRepository userBranchRepository;

    /**
     * Verifies the current user can access the given branch.
     *
     * @param request   the current HTTP request (for correlation info)
     * @param branchId  the branch id to verify access for
     * @throws UnauthorizedBranchAccessException if the user has no access to the branch
     */
    public void verifyAccess(HttpServletRequest request, Long branchId) {
        Long tenantId = resolveTenantId(request);
        Long companyId = resolveCompanyId(request);
        Long userId = resolveUserId();

        if (tenantId == null || companyId == null || userId == null) {
            throw new UnauthorizedBranchAccessException(
                    "Cannot verify branch access without tenant, company, and user context");
        }

        if (hasAllAccess(userId)) {
            return;
        }

        boolean hasAssignment = userBranchRepository.existsByTenantIdAndUserIdAndBranchId(tenantId, userId, branchId);
        if (!hasAssignment) {
            throw new UnauthorizedBranchAccessException(
                    "User " + userId + " is not assigned to branch " + branchId +
                    " within company " + companyId);
        }
    }

    /**
     * Returns true when the user holds a company-wide or branch-level bypass
     * permission that grants access to every branch within the company.
     *
     * <p>TODO: wire this to the permission aspect once company-scoped
     * permission resolution is available. For now it returns false so access
     * is strictly determined by {@code user_branches} assignments.
     *
     * @param userId the user id
     * @return false (placeholder - wired to permissions later)
     */
    private boolean hasAllAccess(Long userId) {
        // Placeholder: check COMPANY_ADMIN / BRANCH_ALL_ACCESS via security context
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (var authority : auth.getAuthorities()) {
                String permission = authority.getAuthority();
                if ("COMPANY_ADMIN".equals(permission)
                        || "BRANCH_ALL_ACCESS".equals(permission)
                        || "ROLE_SUPER_ADMIN".equals(permission)
                        || "ROLE_OWNER".equals(permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Long resolveTenantId(HttpServletRequest request) {
        String tenantId = RequestContext.getTenantId();
        return tenantId != null ? Long.parseLong(tenantId) : null;
    }

    private Long resolveCompanyId(HttpServletRequest request) {
        String companyId = RequestContext.getCompanyId();
        return companyId != null ? Long.parseLong(companyId) : null;
    }

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            String userId = principal.getUserId();
            if (userId != null) {
                try {
                    return Long.parseLong(userId);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}