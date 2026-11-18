package com.erp.platform.identity.application;

import com.erp.platform.security.authentication.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the branch filter that read/list queries should apply.
 *
 * <p>The write path uses {@link CurrentBranchProvider#getRequiredBranchId} and
 * fails when no branch is selected. Reads are different: an operator browsing a
 * list should keep working when the topbar still says "All branches", so this
 * resolver degrades instead of throwing:
 *
 * <ol>
 *   <li>If a branch was explicitly selected via {@code X-Branch-Id}, that branch
 *       is the filter.</li>
 *   <li>If the user holds a company-wide or branch-level bypass permission
 *       ({@code COMPANY_ADMIN}, {@code BRANCH_ALL_ACCESS}, super admin, owner),
 *       no filter is applied and every branch in the company is visible
 *       ({@code null}).</li>
 *   <li>Otherwise the user's primary branch is used, so a branch-scoped user
 *       never sees another branch's rows merely because they left the selector
 *       on "All branches".</li>
 * </ol>
 *
 * <p>The returned value is intended for the {@code (:branchId IS NULL OR
 * e.branchId = :branchId)} predicate: {@code null} means "no branch filter".
 *
 * @since 1.0.0
 */
@Component
public class BranchScopeResolver {

    private final CurrentBranchProvider currentBranchProvider;
    private final UserBranchService userBranchService;

    public BranchScopeResolver(CurrentBranchProvider currentBranchProvider,
                               UserBranchService userBranchService) {
        this.currentBranchProvider = currentBranchProvider;
        this.userBranchService = userBranchService;
    }

    /**
     * Resolves the branch id read queries should filter by.
     *
     * @return the branch id to filter on, or {@code null} for "all branches"
     */
    public Long resolveReadBranchId() {
        Long selected = currentBranchProvider.getCurrentBranchIdOrNull();
        if (selected != null) {
            return selected;
        }
        if (hasAllAccess()) {
            return null;
        }
        Long userId = currentUserId();
        if (userId == null) {
            return null;
        }
        return userBranchService.getUserPrimaryBranchId(userId);
    }

    private boolean hasAllAccess() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (var authority : auth.getAuthorities()) {
            String permission = authority.getAuthority();
            if ("COMPANY_ADMIN".equals(permission)
                    || "BRANCH_ALL_ACCESS".equals(permission)
                    || "ROLE_SUPER_ADMIN".equals(permission)
                    || "ROLE_OWNER".equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private Long currentUserId() {
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