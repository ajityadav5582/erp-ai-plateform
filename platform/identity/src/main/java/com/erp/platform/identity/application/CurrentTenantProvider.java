package com.erp.platform.identity.application;

import com.erp.platform.common.tenancy.TenantContext;
import com.erp.platform.identity.domain.exception.MissingTenantContextException;
import com.erp.platform.security.authentication.AuthenticatedUser;
import jakarta.annotation.Nonnull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides the current tenant ID for the authenticated request.
 *
 * <p>Resolves the tenant ID from the following sources in order:
 * <ol>
 *   <li>{@link TenantContext} thread-local (set by {@code JwtAuthenticationFilter})</li>
 *   <li>{@link AuthenticatedUser} from the Spring Security context</li>
 * </ol>
 *
 * <p>If no tenant ID can be resolved, a {@link MissingTenantContextException} is thrown.
 *
 * @since 1.0.0
 */
@Component
public class CurrentTenantProvider {

    /**
     * Returns the current tenant ID.
     *
     * @return the tenant ID as a {@link Long}
     * @throws MissingTenantContextException if the tenant context is missing
     */
    public Long getCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                return Long.parseLong(tenantId);
            } catch (NumberFormatException ignored) {
                // fall through to security context lookup
            }
        }

        return getTenantIdFromSecurityContext();
    }

    @Nonnull
    private Long getTenantIdFromSecurityContext() {
        org.springframework.security.core.Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            Long tenantId = principal.getTenantId();
            if (tenantId != null) {
                return tenantId;
            }
        }
        throw new MissingTenantContextException();
    }
}
