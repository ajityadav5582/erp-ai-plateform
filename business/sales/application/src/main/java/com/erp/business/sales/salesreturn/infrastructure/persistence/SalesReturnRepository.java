package com.erp.business.sales.salesreturn.infrastructure.persistence;

import com.erp.business.sales.salesreturn.domain.ReturnStatus;
import com.erp.business.sales.salesreturn.domain.SalesReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence port for the {@link SalesReturn} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary.
 */
public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    Optional<SalesReturn> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<SalesReturn> findByTenantIdAndCompanyIdAndCreditNoteNumber(
            Long tenantId, Long companyId, String creditNoteNumber);

    Page<SalesReturn> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesReturn> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                           ReturnStatus status, Pageable pageable);

    Page<SalesReturn> findByTenantIdAndCompanyIdAndInvoiceId(Long tenantId, Long companyId,
                                                              Long invoiceId, Pageable pageable);

    boolean existsByTenantIdAndCompanyIdAndCreditNoteNumber(Long tenantId, Long companyId, String creditNoteNumber);

    @Query("SELECT r FROM SalesReturn r WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
            + "AND LOWER(r.creditNoteNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<SalesReturn> searchByNumber(@Param("tenantId") Long tenantId,
                                     @Param("companyId") Long companyId,
                                     @Param("term") String term,
                                     Pageable pageable);

    /**
     * Sums posted credit notes in a period for the sales register / VAT return.
     * Returns {@code [net, vat, count]}.
     */
    @Query("SELECT COALESCE(SUM(r.subtotalAmount), 0), COALESCE(SUM(r.vatAmount), 0), COUNT(r) "
            + "FROM SalesReturn r WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
            + "AND r.status = :status AND r.returnDate BETWEEN :from AND :to")
    Object[] sumRegister(@Param("tenantId") Long tenantId,
                         @Param("companyId") Long companyId,
                         @Param("status") ReturnStatus status,
                         @Param("from") java.time.LocalDate from,
                         @Param("to") java.time.LocalDate to);

    java.util.List<SalesReturn> findByTenantIdAndCompanyIdAndCbmsSyncedFalse(Long tenantId, Long companyId);

    java.util.List<SalesReturn> findByCbmsSyncedFalseAndStatus(ReturnStatus status);
}
