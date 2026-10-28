package com.erp.business.pos.integration;

import com.erp.business.pos.domain.PosSalesIntegrationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PosSalesIntegrationJobRepository extends JpaRepository<PosSalesIntegrationJob, Long> {

    Optional<PosSalesIntegrationJob> findByTenantIdAndCompanyIdAndPosTransactionId(
            Long tenantId, Long companyId, Long posTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM PosSalesIntegrationJob j WHERE (j.status = :status AND j.nextRetryAt <= :now) OR (j.status = 'IN_PROGRESS' AND j.leaseExpiresAt < :now) ORDER BY j.createdAt ASC")
    List<PosSalesIntegrationJob> findPendingForUpdate(@Param("status") String status, @Param("now") Instant now);
}
