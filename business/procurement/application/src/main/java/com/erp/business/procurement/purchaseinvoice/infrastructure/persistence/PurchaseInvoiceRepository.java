package com.erp.business.procurement.purchaseinvoice.infrastructure.persistence;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Read access to supplier invoices.
 *
 * <p>Every finder is scoped by tenant and company. All companies in a tenant share one
 * tenant id, so a tenant-only lookup would return another company's payable.
 */
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {

    Optional<PurchaseInvoice> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    /**
     * Loads an invoice under a pessimistic write lock.
     *
     * <p>Required when applying a payment: two payments arriving at once would otherwise
     * both read the same balance and both be accepted, over-settling the invoice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM PurchaseInvoice i WHERE i.tenantId = :tenantId "
         + "AND i.companyId = :companyId AND i.id = :id")
    Optional<PurchaseInvoice> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    /**
     * The invoice raised against a delivery, if any. Guards against billing twice.
     */
    Optional<PurchaseInvoice> findByTenantIdAndCompanyIdAndGoodsReceiptId(
            Long tenantId, Long companyId, Long goodsReceiptId);

    @Query("SELECT i FROM PurchaseInvoice i WHERE i.tenantId = :tenantId "
         + "AND i.companyId = :companyId")
    Page<PurchaseInvoice> findInScope(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Invoices still owing money, oldest due date first. This is the accounts payable
     * ageing worklist.
     */
    @Query("SELECT i FROM PurchaseInvoice i WHERE i.tenantId = :tenantId "
         + "AND i.companyId = :companyId AND i.status IN "
         + "(com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus.APPROVED, "
         + " com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoiceStatus.PARTIALLY_PAID)")
    Page<PurchaseInvoice> findOutstanding(Long tenantId, Long companyId, Pageable pageable);

    /**
     * Whether this supplier already has an invoice carrying that number.
     *
     * <p>The number belongs to the supplier, so it is unique per supplier rather than per
     * company: two suppliers both numbering from one is normal and must not collide.
     */
    boolean existsByTenantIdAndCompanyIdAndSupplierIdAndInvoiceNumber(
            Long tenantId, Long companyId, Long supplierId, String invoiceNumber);

    @Query(value = "SELECT pi.id, pi.invoice_date, pi.invoice_number, pi.supplier_name, s.tax_id, "
            + "pi.total_amount, pi.subtotal_amount, pi.tax_amount "
            + "FROM purchase_invoices pi LEFT JOIN suppliers s "
            + "ON s.id = pi.supplier_id AND s.tenant_id = pi.tenant_id "
            + "WHERE pi.tenant_id = :tenantId AND pi.company_id = :companyId "
            + "AND (:fiscalYearId IS NULL OR pi.fiscal_year_id = :fiscalYearId) "
            + "AND (:supplierId IS NULL OR pi.supplier_id = :supplierId) "
            + "AND (:fromDate IS NULL OR pi.invoice_date >= :fromDate) "
            + "AND (:toDate IS NULL OR pi.invoice_date <= :toDate)",
            countQuery = "SELECT COUNT(*) FROM purchase_invoices pi "
                    + "WHERE pi.tenant_id = :tenantId AND pi.company_id = :companyId "
                    + "AND (:fiscalYearId IS NULL OR pi.fiscal_year_id = :fiscalYearId) "
                    + "AND (:supplierId IS NULL OR pi.supplier_id = :supplierId) "
                    + "AND (:fromDate IS NULL OR pi.invoice_date >= :fromDate) "
                    + "AND (:toDate IS NULL OR pi.invoice_date <= :toDate)",
            nativeQuery = true)
    org.springframework.data.domain.Page<Object[]> findPurchaseBook(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("supplierId") Long supplierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable);
}
