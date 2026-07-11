package com.erp.platform.tenant.infrastructure.persistence;

import com.erp.platform.tenant.domain.Tenant;
import com.erp.platform.tenant.domain.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Tenant aggregate.
 *
 * @since 1.0.0
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByTenantId(UUID tenantId);

    Optional<Tenant> findByTenantCode(String tenantCode);

    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

    boolean existsByTenantCode(String tenantCode);

    boolean existsByTenantId(UUID tenantId);
}
