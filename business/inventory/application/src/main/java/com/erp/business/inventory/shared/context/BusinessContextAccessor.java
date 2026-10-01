package com.erp.business.inventory.shared.context;

import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.security.context.BusinessContext;
import org.springframework.stereotype.Component;

/**
 * Single entry point for resolving the tenant and company that the current
 * request is operating on.
 *
 * <p><strong>Why this exists.</strong> Both values are already available as
 * static lookups ({@link TenantContext} and {@link BusinessContext}), but every
 * controller and service was calling them directly, which meant repeating the
 * same null-checking and the same exception message in each one. With a single
 * bean the resolution logic lives in exactly one place, the failure mode is
 * uniform, and it can be mocked in tests.
 *
 * <p><strong>Where the values come from.</strong>
 * <ul>
 *   <li>{@code tenantId} — the signed {@code tenantId} claim of the validated
 *       access token. A client cannot influence it.</li>
 *   <li>{@code companyId} — the {@code X-Company-Id} request header, published
 *       on the request context by
 *       {@code com.erp.platform.security.filter.PlatformJwtAuthenticationFilter}
 *       after the API Gateway forwards the user's company selection.</li>
 * </ul>
 *
 * <p>Both are populated per request by the shared JWT filter and cleared in its
 * {@code finally} block, so this class is safe to use as a singleton bean.
 *
 * @since 1.0.0
 */
@Component
public class BusinessContextAccessor {

    /**
     * Returns the tenant of the current request.
     *
     * @return the authenticated tenant ID
     * @throws IllegalStateException if the request carries no authenticated tenant
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
     * Returns the company selected for the current request.
     *
     * @return the selected company ID
     * @throws IllegalStateException if no company was selected for this request
     */
    public Long getCompanyId() {
        return BusinessContext.requireCompanyId();
    }

    /**
     * Returns the fiscal year selected for the current request.
     *
     * @return the selected fiscal year ID
     * @throws IllegalStateException if no fiscal year was selected for this request
     */
    public Long getFiscalYearId() {
        return BusinessContext.requireFiscalYearId();
    }

    /**
     * Returns the company selected for the current request, or {@code null} when
     * the caller has not selected one. Use this only for endpoints where a
     * company is genuinely optional.
     *
     * @return the selected company ID, or {@code null}
     */
    public Long findCompanyId() {
        return BusinessContext.getCompanyId();
    }
}
