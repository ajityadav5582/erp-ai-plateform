package com.erp.business.pos.session.infrastructure.persistence;

import com.erp.business.pos.domain.PosSession;
import com.erp.business.pos.domain.PosSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link PosSession} aggregate. Every finder is
 * tenant- and company-scoped so a query can never leak across a company boundary.
 */
public interface PosSessionRepository extends JpaRepository<PosSession, Long> {

    Optional<PosSession> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<PosSession> findByTenantIdAndCompanyIdAndSessionNumber(Long tenantId, Long companyId, String sessionNumber);

    Page<PosSession> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<PosSession> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                         PosSessionStatus status, Pageable pageable);

    Page<PosSession> findByTenantIdAndCompanyIdAndRegisterId(Long tenantId, Long companyId,
                                                             Long registerId, Pageable pageable);

    Optional<PosSession> findByTenantIdAndCompanyIdAndRegisterIdAndStatus(Long tenantId, Long companyId,
                                                                          Long registerId, PosSessionStatus status);

    List<PosSession> findByTenantIdAndCompanyIdAndCashierIdAndStatus(Long tenantId, Long companyId,
                                                                     String cashierId, PosSessionStatus status);

    @Query("SELECT s FROM PosSession s WHERE s.tenantId = :tenantId AND s.companyId = :companyId "
            + "AND LOWER(s.sessionNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<PosSession> search(@Param("tenantId") Long tenantId,
                            @Param("companyId") Long companyId,
                            @Param("term") String term,
                            Pageable pageable);
}
