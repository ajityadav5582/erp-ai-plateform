package com.erp.business.procurement.shared.infrastructure.persistence;

import com.erp.business.procurement.shared.domain.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    Optional<DocumentSequence> findByTenantIdAndCompanyIdAndDocumentTypeAndPeriodKey(
            Long tenantId, Long companyId, String documentType, String periodKey);

    /**
     * Pessimistic write lock on the counter row.
     *
     * <p>Load-increment-save without this lock is a lost-update race: two concurrent
     * requests read the same value and issue the same document number. Duplicate numbers
     * make the audit trail ambiguous, so the lock is mandatory rather than an optimisation.
     *
     * <p>The caller must already hold a transaction; the lock is released at commit.
     */
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
