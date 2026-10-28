package com.erp.business.pos.transaction.infrastructure.persistence;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link PosTransaction} aggregate. Every finder is
 * tenant- and company-scoped so a query can never leak across a company boundary.
 */
public interface PosTransactionRepository extends JpaRepository<PosTransaction, Long> {

    Optional<PosTransaction> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<PosTransaction> findByTenantIdAndCompanyIdAndTransactionNumber(Long tenantId, Long companyId,
                                                                            String transactionNumber);

    Page<PosTransaction> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<PosTransaction> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                             PosTransactionStatus status, Pageable pageable);

    Page<PosTransaction> findByTenantIdAndCompanyIdAndSessionId(Long tenantId, Long companyId,
                                                                Long sessionId, Pageable pageable);

    Page<PosTransaction> findByTenantIdAndCompanyIdAndRegisterId(Long tenantId, Long companyId,
                                                                 Long registerId, Pageable pageable);

    Page<PosTransaction> findByTenantIdAndCompanyIdAndCustomerId(Long tenantId, Long companyId,
                                                                 Long customerId, Pageable pageable);

    @Query("SELECT t FROM PosTransaction t WHERE t.tenantId = :tenantId AND t.companyId = :companyId "
            + "AND LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<PosTransaction> search(@Param("tenantId") Long tenantId,
                                @Param("companyId") Long companyId,
                                @Param("term") String term,
                                Pageable pageable);

    // -----------------------------------------------------------------------
    // CBMS outbox (used by PosCbmsSyncWorker)
    // -----------------------------------------------------------------------

    /**
     * Completed sales not yet pushed to IRD CBMS — the outbox query.
     *
     * <p>Deliberately unscoped: the scheduler has no tenant context, and the
     * outbox must drain every company's pending bills, exactly as the Sales
     * module's worker does.
     */
    List<PosTransaction> findByCbmsSyncedFalseAndStatus(PosTransactionStatus status);

    /** Every transaction still awaiting a CBMS push, for the status endpoint. */
    List<PosTransaction> findByTenantIdAndCompanyIdAndCbmsSyncedFalse(Long tenantId, Long companyId);
}
