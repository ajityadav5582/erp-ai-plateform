package com.erp.platform.tenant.application;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantCreatedEvent;
import com.erp.platform.tenant.domain.IsolationStrategy;
import com.erp.platform.tenant.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Command handler for creating tenants.
 *
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class CreateTenantCommandHandler {

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TenantResponse handle(CreateTenantRequest request) {
        // Check if tenant code already exists
        if (tenantRepository.existsByTenantCode(request.tenantCode())) {
            throw new IllegalArgumentException("Tenant code already exists: " + request.tenantCode());
        }

        // Create tenant
        Tenant tenant = Tenant.create(
                UUID.randomUUID(),
                request.tenantCode(),
                request.legalName(),
                request.displayName(),
                request.email(),
                request.phone(),
                request.website(),
                request.timezone(),
                request.currency(),
                request.language(),
                IsolationStrategy.valueOf(request.isolationStrategy())
        );

        // Save tenant
        Tenant savedTenant = tenantRepository.save(tenant);

        // Publish event (timestamp provided by application layer)
        TenantCreatedEvent event = TenantCreatedEvent.of(
                savedTenant.getTenantId(),
                savedTenant.getTenantCode(),
                savedTenant.getLegalName(),
                savedTenant.getDisplayName(),
                savedTenant.getIsolationStrategy().name(),
                Instant.now()
        );
        eventPublisher.publishEvent(event);

        return TenantResponse.from(savedTenant);
    }
}
