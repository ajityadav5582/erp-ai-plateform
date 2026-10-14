package com.erp.business.sales.invoice.infrastructure.persistence;

import com.erp.business.sales.invoice.domain.SalesInvoice;
import com.erp.business.sales.invoice.domain.SalesInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link SalesInvoice} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary.
 */
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {

    Optional<SalesInvoice> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<SalesInvoice> findByTenantIdAndCompanyIdAndInvoiceNumber(
            Long tenantId, Long companyId, String invoiceNumber);

    List<SalesInvoice> findByTenantIdAndCompanyIdAndSalesOrderId(
            Long tenantId, Long companyId, Long salesOrderId);

    Optional<SalesInvoice> findFirstByTenantIdAndCompanyIdAndSalesOrderIdOrderByInvoiceDateDesc(
            Long tenantId, Long companyId, Long salesOrderId);

    Page<SalesInvoice> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesInvoice> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                            SalesInvoiceStatus status, Pageable pageable);

    Page<SalesInvoice> findByTenantIdAndCompanyIdAndCustomerId(Long tenantId, Long companyId,
                                                                Long customerId, Pageable pageable);

    /** Invoices still owing money, for the receivables ageing / collection view. */
    List<SalesInvoice> findByTenantIdAndCompanyIdAndStatusInAndBalanceAmountGreaterThanOrderByIdDesc(
            Long tenantId, Long companyId, List<SalesInvoiceStatus> statuses,
            java.math.BigDecimal balanceThreshold);

    boolean existsByTenantIdAndCompanyIdAndInvoiceNumber(Long tenantId, Long companyId, String invoiceNumber);

    @Query("SELECT DISTINCT i.salesOrderId FROM SalesInvoice i WHERE i.tenantId = :tenantId AND i.companyId = :companyId AND i.status != 'CANCELLED'")
    List<Long> findInvoicedSalesOrderIds(@Param("tenantId") Long tenantId, @Param("companyId") Long companyId);

    @Query("SELECT i FROM SalesInvoice i WHERE i.tenantId = :tenantId AND i.companyId = :companyId "
            + "AND LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<SalesInvoice> searchByNumber(@Param("tenantId") Long tenantId,
                                      @Param("companyId") Long companyId,
                                      @Param("term") String term,
                                      Pageable pageable);

    /**
     * Sums the VAT register breakdown for issued invoices in a period, for the
     * sales register / VAT return. Returns {@code [taxable, exempt, zeroRated, vat, count]}.
     */
    @Query("SELECT COALESCE(SUM(i.taxableAmount), 0), COALESCE(SUM(i.exemptAmount), 0), "
            + "COALESCE(SUM(i.zeroRatedAmount), 0), COALESCE(SUM(i.vatAmount), 0), COUNT(i) "
            + "FROM SalesInvoice i WHERE i.tenantId = :tenantId AND i.companyId = :companyId "
            + "AND i.status IN :statuses AND i.invoiceDate BETWEEN :from AND :to")
    Object[] sumRegister(@Param("tenantId") Long tenantId,
                         @Param("companyId") Long companyId,
                         @Param("statuses") List<SalesInvoiceStatus> statuses,
                         @Param("from") java.time.LocalDate from,
                         @Param("to") java.time.LocalDate to);

    List<SalesInvoice> findByTenantIdAndCompanyIdAndCbmsSyncedFalse(Long tenantId, Long companyId);

    List<SalesInvoice> findByCbmsSyncedFalseAndStatusIn(List<SalesInvoiceStatus> statuses);
}
