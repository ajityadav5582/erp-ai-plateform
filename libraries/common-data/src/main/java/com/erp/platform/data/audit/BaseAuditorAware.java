package com.erp.platform.data.audit;

import com.erp.platform.common.context.RequestContext;
import org.springframework.stereotype.Component;

/**
 * Base auditor aware implementation.
 *
 * <p>Provides the current user ID from the request context for JPA auditing.
 * This is the default implementation that should be used by microservices.
 *
 * @since 1.0.0
 */
@Component
public class BaseAuditorAware implements AuditorAware {

    @Override
    public java.util.Optional<String> getCurrentAuditor() {
        String userId = RequestContext.getUserId();
        return java.util.Optional.ofNullable(userId);
    }
}
