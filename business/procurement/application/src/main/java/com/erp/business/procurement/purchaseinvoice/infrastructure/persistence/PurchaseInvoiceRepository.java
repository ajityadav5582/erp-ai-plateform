package com.erp.business.procurement.purchaseinvoice.infrastructure.persistence;

import com.erp.business.procurement.purchaseinvoice.domain.PurchaseInvoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
