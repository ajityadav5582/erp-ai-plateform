package com.erp.platform.identity.interfaces.rest.filter;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.BranchContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.application.BranchAccessEvaluator;
import com.erp.platform.identity.domain.exception.BranchOperationException;
import com.erp.platform.identity.domain.exception.BranchNotFoundException;
import com.erp.platform.identity.domain.exception.MissingBranchContextException;
import com.erp.platform.identity.domain.exception.UnauthorizedBranchAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Branch scoping filter.
 *
 * <p>Reads the {@code X-Branch-Id} header (set by the API gateway) and resolves
 * it into the branch-scoped thread-local context.
 *
 * <p>When the header is present it is validated against the user's branch
 * assignments within the (tenantId, companyId) scope. An unauthorized branch
 * yields {@code 403 BRANCH_ACCESS_DENIED}; a malformed value yields
 * {@code 400 BRANCH_ID_INVALID}.
 *
 * <p>When the header is absent the branch context simply remains unset, and
 * read operations may fall back to the user's primary branch or the company
 * default branch, while write operations resolve a required branch via
 * {@code CurrentBranchProvider#getRequiredBranchId}.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchFilter extends OncePerRequestFilter {

    /** Header carrying the selected branch id. */
    public static final String HEADER_BRANCH_ID = "X-Branch-Id";

    private final BranchAccessEvaluator branchAccessEvaluator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER_BRANCH_ID);
            if (header != null && !header.isBlank()) {
                Long branchId = parseBranchId(header);
                if (branchId == null) {
                    throw new BranchOperationException(HttpStatus.BAD_REQUEST, "BRANCH_ID_INVALID",
                            "Branch id must be a positive number", null);
                }
                branchAccessEvaluator.verifyAccess(request, branchId);
                BranchContext.setBranchId(branchId);
            }

            filterChain.doFilter(request, response);
        } finally {
            BranchContext.clear();
        }
    }

    private Long parseBranchId(String header) {
        try {
            long parsed = Long.parseLong(header.trim());
            if (parsed <= 0) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}