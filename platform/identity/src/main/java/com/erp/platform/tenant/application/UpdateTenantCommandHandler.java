package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantNotFoundException;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for updating an existing tenant.
 *
 * <p>Applies the non-null fields from the {@link UpdateTenantRequest} to the
 * tenant aggregate using its domain behaviour methods. Immutable attributes
 * (tenant code, legal name, isolation strategy) are intentionally not modified.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class UpdateTenantCommandHandler {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse handle(Long id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));

        tenant.updateBranding(request.displayName(), request.logoUrl(), request.faviconUrl());
        tenant.updateContactInfo(request.email(), request.phone(), request.website());
        tenant.updateLocalization(request.timezone(), request.currency(), request.language());

        if (request.subscriptionPlanId() != null) {
            tenant.assignSubscriptionPlan(request.subscriptionPlanId());
        }

        Tenant savedTenant = tenantRepository.save(tenant);
        return TenantResponse.from(savedTenant);
    }
}
