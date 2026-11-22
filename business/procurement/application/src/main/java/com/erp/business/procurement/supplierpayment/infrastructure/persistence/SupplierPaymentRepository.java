package com.erp.business.procurement.supplierpayment.infrastructure.persistence;

import com.erp.business.procurement.supplierpayment.domain.SupplierPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Read access to supplier payments.
 *
 * <p>Every finder is scoped by tenant and company.
 */
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    Optional<SupplierPayment> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    Optional<SupplierPayment> findByTenantIdAndCompanyIdAndReferenceNo(
            Long tenantId, Long companyId, String referenceNo);

    @Query("SELECT DISTINCT p FROM SupplierPayment p LEFT JOIN FETCH p.allocations "
         + "WHERE p.tenantId = :tenantId AND p.companyId = :companyId AND p.id = :id")
    Optional<SupplierPayment> findWithAllocationsById(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    /**
     * Loads a payment under a pessimistic write lock, for reversal.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SupplierPayment p WHERE p.tenantId = :tenantId "
         + "AND p.companyId = :companyId AND p.id = :id")
    Optional<SupplierPayment> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    @Query("SELECT p FROM SupplierPayment p WHERE p.tenantId = :tenantId "
         + "AND p.companyId = :companyId")
    Page<SupplierPayment> findInScope(Long tenantId, Long companyId, Pageable pageable);

    /** The payments made against one invoice, whether directly or via multi-invoice allocation. */
    @Query("SELECT DISTINCT p FROM SupplierPayment p LEFT JOIN p.allocations a "
         + "WHERE p.tenantId = :tenantId AND p.companyId = :companyId "
         + "AND (p.purchaseInvoiceId = :invoiceId OR a.purchaseInvoiceId = :invoiceId)")
    Page<SupplierPayment> findByInvoice(Long tenantId, Long companyId, Long invoiceId,
                                        Pageable pageable);

    /**
     * Payments with unallocated funds (advance credits) available for a supplier.
     */
    @Query("SELECT p FROM SupplierPayment p WHERE p.tenantId = :tenantId "
         + "AND p.companyId = :companyId AND p.supplierId = :supplierId "
         + "AND p.status = com.erp.business.procurement.supplierpayment.domain.SupplierPaymentStatus.POSTED "
         + "AND p.unallocatedAmount > 0 ORDER BY p.paymentDate ASC, p.id ASC")
    java.util.List<SupplierPayment> findAvailableAdvances(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("supplierId") Long supplierId);

    java.util.List<SupplierPayment> findByTenantIdAndCompanyIdAndSupplierId(
            Long tenantId, Long companyId, Long supplierId);

    /**
     * The highest numeric suffix already issued for a payment prefix in this scope.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.paymentNumber, :prefixLength + 1) AS java.lang.Long)), 0) "
         + "FROM SupplierPayment p "
         + "WHERE p.tenantId = :tenantId AND p.companyId = :companyId "
         + "AND p.paymentNumber LIKE CONCAT(:prefix, '%')")
    Long findHighestIssuedNumber(@Param("tenantId") Long tenantId,
                                 @Param("companyId") Long companyId,
                                 @Param("prefix") String prefix,
                                 @Param("prefixLength") int prefixLength);
}
