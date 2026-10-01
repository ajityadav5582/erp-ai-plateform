package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantStatus;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query handler for listing tenants.
 *
 * <p>Supports pagination and optional filtering by {@link TenantStatus}.
 * Sorting is delegated to the provided {@link Pageable} (e.g. {@code ?sort=createdAt,desc}).
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class ListTenantsQueryHandler {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Page<TenantResponse> handle(TenantStatus status, Pageable pageable) {
        Page<Tenant> page = (status != null)
                ? tenantRepository.findByStatus(status, pageable)
                : tenantRepository.findAll(pageable);
        return page.map(TenantResponse::from);
    }
}
