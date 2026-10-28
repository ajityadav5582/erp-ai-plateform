package com.erp.platform.shared.infrastructure.persistence;

import com.erp.platform.shared.domain.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    Optional<DocumentSequence> findByTenantIdAndCompanyIdAndDocumentTypeAndPeriodKey(
            Long tenantId, Long companyId, String documentType, String periodKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DocumentSequence s WHERE s.tenantId = :tenantId " +
            "AND s.companyId = :companyId AND s.documentType = :documentType " +
            "AND s.periodKey = :periodKey")
    Optional<DocumentSequence> findForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("documentType") String documentType,
            @Param("periodKey") String periodKey);
}
