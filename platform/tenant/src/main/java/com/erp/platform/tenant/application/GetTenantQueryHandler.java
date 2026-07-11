package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.TenantNotFoundException;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query handler for getting tenants.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class GetTenantQueryHandler {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public TenantResponse handle(Long id) {
        return tenantRepository.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }
}
