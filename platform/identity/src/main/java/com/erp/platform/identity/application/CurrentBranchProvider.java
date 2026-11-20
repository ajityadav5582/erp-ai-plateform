package com.erp.platform.identity.application;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.BranchContext;
import com.erp.platform.identity.domain.exception.BranchOperationException;
import com.erp.platform.identity.domain.exception.MissingBranchContextException;
import com.erp.platform.identity.domain.exception.UnauthorizedBranchAccessException;
import org.springframework.stereotype.Component;

/**
 * Provides the current branch ID for the authenticated request.
 *
 * <p>The branch is the third level of scoping after tenant and company. The
 * active branch is determined from the {@code X-Branch-Id} request header, which
 * the gateway/JWT filter stores in {@link RequestContext}. This component
 * exposes it to the application layer and performs access verification.
 *
 * <p>The value is trusted because it is only ever populated from the header by
 * the {@code BranchFilter}, never from a request body or query parameter.
 * Callers must still combine it with the tenant id and company id, so a header
 * pointing at another tenant's or company's branch cannot be used to read or
 * mutate that branch's data.
 *
 * @since 1.0.0
 */
@Component
public class CurrentBranchProvider {

    /**
     * Returns the currently selected branch ID.
     *
     * @return the branch ID as a {@link Long}
     * @throws MissingBranchContextException if no branch is selected for this request
     */
    public Long getCurrentBranchId() {
        Long branchId = BranchContext.getBranchId();
        if (branchId == null) {
            throw new MissingBranchContextException();
        }
        return branchId;
    }

    /**
     * Returns the currently selected branch ID, or {@code null} when absent.
     *
     * @return the branch ID, or {@code null} when the request carries none
     */
    public Long getCurrentBranchIdOrNull() {
        return BranchContext.getBranchId();
    }

    /**
     * Returns the currently selected branch ID, throwing a 400 error if null
     * on write operations. This is intended for use in create operations where
     * a branch must always be explicitly selected.
     *
     * @return the branch ID
     * @throws MissingBranchContextException if branch context is null
     */
    public Long getRequiredBranchId() {
        Long branchId = BranchContext.getBranchId();
        if (branchId == null) {
            throw new MissingBranchContextException();
        }
        return branchId;
    }
}