package com.erp.platform.data.audit;

import com.erp.platform.common.tenancy.TenantContextHolder;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Auditing configuration for the platform.
 *
 * <p>Provides the auditor aware implementation that populates
 * {@code createdBy} and {@code updatedBy} fields automatically.
 *
 * @since 1.0.0
 */
@EnableJpaAuditing
public class AuditingConfig implements AuditorAware<String> {

    /**
     * Returns the current auditor (user) from the request context.
     *
     * <p>If no user is available in the request context, returns
     * "system" as the default auditor.
     *
     * @return the current auditor
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        String userId = com.erp.platform.common.context.RequestContext.getUserId();
        if (userId != null && !userId.isBlank()) {
            return Optional.of(userId);
        }

        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            return Optional.of("tenant:" + tenantId);
        }

        return Optional.of("system");
    }
}
