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

    @Query(value = "SELECT COALESCE(SUM(total_amount),0), COALESCE(SUM(taxable_amount),0), COALESCE(SUM(vat_amount),0), " +
            "COALESCE(SUM(CASE WHEN payment_method='CASH' THEN total_amount ELSE 0 END),0), " +
            "COALESCE(SUM(CASE WHEN payment_method='QR' THEN total_amount ELSE 0 END),0), " +
            "COALESCE(SUM(CASE WHEN payment_method='CARD' THEN total_amount ELSE 0 END),0), " +
            "MIN(transaction_number), MAX(transaction_number), COUNT(*) " +
            "FROM pos_transactions WHERE session_id = :sessionId", nativeQuery = true)
    Object[] aggregateBySession(@Param("sessionId") Long sessionId);

    @Query(value = "SELECT transaction_number, transaction_date, customer_name, customer_pan_number, " +
            "total_amount, exempt_amount, 0, taxable_amount, vat_amount, status " +
            "FROM pos_transactions WHERE company_id = :companyId AND status IN ('COMPLETED','CREDIT_NOTE') " +
            "ORDER BY transaction_date", nativeQuery = true)
    List<Object[]> findSalesBook(@Param("companyId") Long companyId);

    @Query(value = "SELECT transaction_number, transaction_date, customer_name, customer_pan_number, " +
            "total_amount, taxable_amount, vat_amount, void_reason, cbms_synced " +
            "FROM pos_transactions WHERE company_id = :companyId AND status = 'CREDIT_NOTE' " +
            "ORDER BY transaction_date", nativeQuery = true)
    List<Object[]> findCreditNotes(@Param("companyId") Long companyId);
}
