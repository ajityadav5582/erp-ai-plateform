package com.erp.business.procurement.purchaserequisition.infrastructure.persistence;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisition;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequisitionRepository extends JpaRepository<PurchaseRequisition, Long> {

    /**
     * Every finder that takes an id from a path variable or body requires companyId
     * alongside tenantId. Loading on tenant alone returns another company's document,
     * because every company in a tenant shares one tenantId.
     */
    Optional<PurchaseRequisition> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    Optional<PurchaseRequisition> findByTenantIdAndCompanyIdAndPrNumber(
            Long tenantId, Long companyId, String prNumber);

    Page<PurchaseRequisition> findByTenantIdAndCompanyId(
            Long tenantId, Long companyId, Pageable pageable);

    Page<PurchaseRequisition> findByTenantIdAndCompanyIdAndBranchId(
            Long tenantId, Long companyId, Long branchId, Pageable pageable);

    Page<PurchaseRequisition> findByTenantIdAndCompanyIdAndStatus(
            Long tenantId, Long companyId, PurchaseRequisitionStatus status, Pageable pageable);

    Page<PurchaseRequisition> findByTenantIdAndCompanyIdAndBranchIdAndStatus(
            Long tenantId, Long companyId, Long branchId, PurchaseRequisitionStatus status,
            Pageable pageable);

    /**
     * "My requisitions", newest first.
     *
     * <p>Scoped by company as well as requester, because a user who has worked in
     * two companies of the same tenant has one numeric user id and must not see the
     * other company's documents under the same person.
     */
    Page<PurchaseRequisition> findByTenantIdAndCompanyIdAndRequestedByOrderByCreatedAtDesc(
            Long tenantId, Long companyId, Long requestedBy, Pageable pageable);

    /**
     * Loads several requisitions at once, preserving nothing about their order.
     *
     * <p>Used to turn a page of approval-step ids into the documents behind them.
     * Without this the inbox issues one query per row, which turns a twenty-item
     * queue into twenty round trips.
     */
    List<PurchaseRequisition> findByTenantIdAndCompanyIdAndIdIn(
            Long tenantId, Long companyId, Collection<Long> ids);

    /**
     * Approver inbox: everything awaiting a decision, oldest first.
     */
    Page<PurchaseRequisition> findByTenantIdAndCompanyIdAndStatusOrderBySubmittedAtAsc(
            Long tenantId, Long companyId, PurchaseRequisitionStatus status, Pageable pageable);

    List<PurchaseRequisition> findByTenantIdAndCompanyIdAndStatusIn(
            Long tenantId, Long companyId, Collection<PurchaseRequisitionStatus> statuses);

    boolean existsByTenantIdAndCompanyIdAndPrNumber(
            Long tenantId, Long companyId, String prNumber);

    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);

    /**
     * The highest document number already issued in this scope, as its numeric
     * suffix.
     *
     * <p>Exists to make numbering survive a change of period key. Switching the
     * partition from a calendar year to a fiscal year creates a
     * {@code document_sequences} row that has never been used, and a fresh counter
     * starts at zero - so the first requisition of the new fiscal year is issued
     * PR-000001, which already exists under the old key, and the unique constraint
     * on (tenant, company, pr_number) rejects the whole transaction. Seeding the
     * new counter above the highest number ever issued in this scope makes the
     * change safe and also survives a counter row being deleted by hand.
     *
     * <p>Deliberately scoped to the company and not to the period: the point is to
     * avoid ever reissuing a number this company has used, and only a whole-scope
     * maximum can guarantee that.
     *
     * @param prefix the document prefix, for example {@code PR-}
     * @return the highest suffix in use, or 0 when none has been issued
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.prNumber, :prefixLength + 1) AS java.lang.Long)), 0) " +
           "FROM PurchaseRequisition r WHERE r.tenantId = :tenantId AND r.companyId = :companyId " +
           "AND r.prNumber LIKE CONCAT(:prefix, '%')")
    Long findHighestIssuedNumber(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("prefix") String prefix,
            @Param("prefixLength") int prefixLength);

    /**
     * Budget consumed per branch in a period, summed from approved totals.
     * Only budget-consuming states count; a draft is a proposal, not a commitment.
     */
    @Query("SELECT COALESCE(SUM(r.approvedTotalAmount), 0) FROM PurchaseRequisition r " +
           "WHERE r.tenantId = :tenantId AND r.companyId = :companyId " +
           "AND r.branchId = :branchId AND r.fiscalYearId = :fiscalYearId " +
           "AND r.status IN :statuses")
    BigDecimal sumApprovedAmountForBranchAndFiscalYear(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("statuses") Collection<PurchaseRequisitionStatus> statuses);

    /**
     * Pessimistic write lock for the approval and cancellation paths.
     *
     * <p>Two approvers acting on the same document must serialise rather than both
     * loading a stale version. The entity already carries a version column for the
     * optimistic path; this lock covers the read-modify-decide-write window.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PurchaseRequisition r WHERE r.tenantId = :tenantId " +
           "AND r.companyId = :companyId AND r.id = :id")
    Optional<PurchaseRequisition> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    /**
     * Number search. The ESCAPE clause is load-bearing: without it a caller searching
     * for % or _ has those characters reinterpreted as wildcards and matches everything.
     * The service escapes them before they reach here.
     */
    @Query("SELECT r FROM PurchaseRequisition r WHERE r.tenantId = :tenantId " +
           "AND r.companyId = :companyId AND " +
           "LOWER(r.prNumber) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'")
    Page<PurchaseRequisition> searchByNumber(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("query") String query,
            Pageable pageable);
}
