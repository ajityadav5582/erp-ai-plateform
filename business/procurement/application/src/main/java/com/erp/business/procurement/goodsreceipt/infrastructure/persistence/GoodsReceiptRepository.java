package com.erp.business.procurement.goodsreceipt.infrastructure.persistence;

import com.erp.business.procurement.goodsreceipt.domain.GoodsReceipt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Read access to goods receipt notes.
 *
 * <p>Every finder is scoped by tenant and company. All companies in a tenant share one
 * tenant id, so a tenant-only lookup would return another company's warehouse document.
 */
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    Optional<GoodsReceipt> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    /**
     * Loads a receipt together with its lines in one query.
     *
     * <p>Used to build the stock-movement batch outside a transaction (the retry
     * path), where a lazy {@code lines} collection on a detached entity would
     * fail. The join fetch loads them eagerly so the batch can be assembled
     * after the read transaction has closed.
     */
    @Query("SELECT r FROM GoodsReceipt r LEFT JOIN FETCH r.lines "
         + "WHERE r.tenantId = :tenantId AND r.companyId = :companyId AND r.id = :id")
    Optional<GoodsReceipt> findWithLinesByTenantIdAndCompanyIdAndId(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    /**
     * Loads a receipt under a pessimistic write lock, for cancellation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM GoodsReceipt r WHERE r.tenantId = :tenantId "
         + "AND r.companyId = :companyId AND r.id = :id")
    Optional<GoodsReceipt> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    /**
     * The receipts raised against an order, newest first.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param orderId   the purchase order id
     * @param pageable  paging and sorting
     * @return the receipts for that order
     */
    @Query("SELECT r FROM GoodsReceipt r WHERE r.tenantId = :tenantId "
         + "AND r.companyId = :companyId AND r.purchaseOrderId = :orderId")
    Page<GoodsReceipt> findByOrder(Long tenantId, Long companyId, Long orderId, Pageable pageable);

    /**
     * Every receipt in the caller's scope, newest first.
     */
    @Query("SELECT r FROM GoodsReceipt r WHERE r.tenantId = :tenantId AND r.companyId = :companyId")
    Page<GoodsReceipt> findInScope(Long tenantId, Long companyId, Pageable pageable);

    /**
     * The receipts raised against a given goods receipt. Guards against invoicing the
     * same delivery twice.
     */
    @Query("SELECT COUNT(i) > 0 FROM PurchaseInvoice i "
         + "WHERE i.tenantId = :tenantId AND i.companyId = :companyId "
         + "AND i.goodsReceiptId = :goodsReceiptId AND i.status <> 'CANCELLED'")
    boolean invoiceExistsForReceipt(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("goodsReceiptId") Long goodsReceiptId);

    /**
     * The highest numeric suffix already issued for a GRN prefix in this scope.
     *
     * <p>Seeds a newly created period counter so a fiscal-year boundary continues the
     * series rather than reissuing a number the unique constraint then rejects.
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(r.grnNumber, :prefixLength + 1) AS java.lang.Long)), 0) "
         + "FROM GoodsReceipt r "
         + "WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
         + "AND r.grnNumber LIKE CONCAT(:prefix, '%')")
    Long findHighestIssuedNumber(@Param("tenantId") Long tenantId,
                                 @Param("companyId") Long companyId,
                                 @Param("prefix") String prefix,
                                 @Param("prefixLength") int prefixLength);
}
