package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantNotFoundException;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for deleting a tenant.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class DeleteTenantCommandHandler {

    private final TenantRepository tenantRepository;

    @Transactional
    public void handle(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        tenantRepository.delete(tenant);
    }
}
