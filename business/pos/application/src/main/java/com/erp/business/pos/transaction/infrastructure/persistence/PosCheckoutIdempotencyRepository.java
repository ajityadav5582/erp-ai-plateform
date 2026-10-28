package com.erp.business.pos.transaction.infrastructure.persistence;

import com.erp.business.pos.domain.PosCheckoutIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PosCheckoutIdempotencyRepository extends JpaRepository<PosCheckoutIdempotency, Long> {
    Optional<PosCheckoutIdempotency> findByTenantIdAndCompanyIdAndUserIdAndIdempotencyKey(
            Long tenantId, Long companyId, String userId, String idempotencyKey);
}
