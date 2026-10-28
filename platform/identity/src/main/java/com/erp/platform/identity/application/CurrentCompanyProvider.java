package com.erp.platform.identity.application;

import com.erp.platform.common.context.RequestContext;
import com.erp.platform.identity.domain.exception.MissingCompanyContextException;
import org.springframework.stereotype.Component;

/**
 * Provides the company (legal entity) selected for the current request.
 *
 * <p>The company is not part of the JWT: the user picks an active company after
 * login and every subsequent request carries it in the {@code X-Company-Id}
 * header. The gateway / JWT filter stores that value in {@link RequestContext},
 * and this component exposes it to the application layer.
 *
 * <p>The value is trusted because it is only ever populated from the header by
 * the authentication filter, never from a request body or query parameter.
 * Callers must still combine it with the tenant id, so a header pointing at
 * another tenant's company cannot be used to read that company's branches.
 *
 * @since 1.0.0
 */
@Component
public class CurrentCompanyProvider {

    /**
     * Returns the currently selected company ID.
     *
     * @return the company ID as a {@link Long}
     * @throws MissingCompanyContextException if no company is selected for this request
     */
    public Long getCurrentCompanyId() {
        String companyId = RequestContext.getCompanyId();
        if (companyId == null || companyId.isBlank()) {
            throw new MissingCompanyContextException();
        }
        try {
            long parsed = Long.parseLong(companyId.trim());
            if (parsed <= 0) {
                throw new MissingCompanyContextException();
            }
            return parsed;
        } catch (NumberFormatException notANumber) {
            throw new MissingCompanyContextException();
        }
    }

    /**
     * Returns the currently selected company ID, or {@code null} when absent.
     *
     * <p>For read paths that should still work before a company is chosen, for
     * example the company switcher itself.
     *
     * @return the company ID, or {@code null} when the request carries none
     */
    public Long getCurrentCompanyIdOrNull() {
        String companyId = RequestContext.getCompanyId();
        if (companyId == null || companyId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(companyId.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
