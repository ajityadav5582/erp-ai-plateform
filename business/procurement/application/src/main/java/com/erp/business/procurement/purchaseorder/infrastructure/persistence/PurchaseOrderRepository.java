package com.erp.business.procurement.purchaseorder.infrastructure.persistence;

import com.erp.business.procurement.purchaseorder.domain.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Read access to purchase orders.
 *
 * <p>Originally present only so that the requisition module could verify a purchase
 * order before recording an award against it. It exists because an order award writes
 * a {@code purchase_order_id} onto {@code PurchaseRequisitionOrderLink} with no
 * database foreign key between the two documents - a constraint there would couple
 * two aggregates that are meant to be loadable and versionable independently. Losing
 * the constraint means the link must be validated in the application layer instead, and
 * an unqualified {@code findById} would happily resolve another tenant's or another
 * company's order.
 *
 * <p>It now also backs the order book itself, via {@link #findInScope}. That is why
 * every finder here is tenant- and company-scoped: all companies in a tenant share one
 * {@code tenantId}, so a tenant-only list would show one company's spend to another.
 */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Loads a purchase order only when it belongs to the given tenant and company.
     *
     * <p>Every finder that takes an id originating from a request body must be
     * company-scoped alongside tenant-scoped: all companies in a tenant share one
     * {@code tenantId}, so loading on tenant alone returns another company's
     * document.
     *
     * @param tenantId  the tenant from the signed token claim
     * @param companyId the company from the {@code X-Company-Id} header
     * @param id        the purchase order id supplied by the caller
     * @return the order, or empty when it does not exist in that scope
     */
    Optional<PurchaseOrder> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    /**
     * Loads a purchase order under a pessimistic write lock.
     *
     * <p>Every lifecycle transition goes through here. The aggregate already refuses an
     * illegal transition, but two requests that both read SUBMITTED before either has
     * written would both pass that check and both stamp the approval. The lock makes the
     * second one wait and then read the committed status, so it is refused instead.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PurchaseOrder o WHERE o.tenantId = :tenantId "
         + "AND o.companyId = :companyId AND o.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("id") Long id);

    /**
     * Existence check in the caller's own scope, for validating an award before
     * any aggregate state is mutated.
     *
     * @param tenantId  the tenant from the signed token claim
     * @param companyId the company from the {@code X-Company-Id} header
     * @param id        the purchase order id supplied by the caller
     * @return {@code true} when an order with that id exists in this scope
     */
    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    /**
     * Every order in the caller's scope.
     *
     * <p>Backs the order book. Scoped by tenant <em>and</em> company, so a company
     * header switching between companies within one tenant cannot widen the list.
     *
     * <p>Sorting is whatever the caller asks for rather than being fixed here, so the
     * screen can offer "newest first" and "largest first" off the same query. An order
     * with no sort requested falls back to insertion order, which is stable and
     * predictable but not meaningful; the controller always supplies a default.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param pageable  paging and sorting
     * @return the orders in this scope
     */
    @Query("SELECT o FROM PurchaseOrder o WHERE o.tenantId = :tenantId AND o.companyId = :companyId")
    Page<PurchaseOrder> findInScope(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            Pageable pageable);

    /**
     * The orders in the caller's scope that are still short of full receipt.
     *
     * <p>Backs the "outstanding deliveries" tab: an order still owing goods is one whose
     * header has not reached {@code RECEIVED}. Filtering in the database rather than
     * after paging is the whole point - an outstanding-only worklist that paged first
     * and filtered in memory would return short pages and misreport the total.
     *
     * <p>Terminal states are excluded as well as {@code RECEIVED}: a cancelled or
     * rejected order will never be delivered, so it is not outstanding work.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param pageable  paging and sorting
     * @return orders still awaiting receipt
     */
    @Query("SELECT o FROM PurchaseOrder o WHERE o.tenantId = :tenantId AND o.companyId = :companyId "
         + "AND o.status NOT IN ('RECEIVED', 'CLOSED', 'CANCELLED', 'REJECTED')")
    Page<PurchaseOrder> findOutstandingInScope(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            Pageable pageable);

    /**
     * The order already produced by converting a given requisition, if any.
     *
     * <p>Backs the duplicate-conversion guard. Company-scoped alongside tenant-scoped
     * because every company in a tenant shares one tenant id, so a tenant-only lookup
     * would report another company's order as a duplicate and refuse a legitimate
     * conversion.
     *
     * <p>Advisory on its own - two concurrent requests both read empty. The unique key
     * on {@code (tenant_id, company_id, source_purchase_requisition_id)} is what
     * actually prevents the second order; this exists so the loser gets a message
     * about the duplication rather than a constraint violation.
     *
     * @param tenantId     the tenant from the signed token claim
     * @param companyId    the company from the {@code X-Company-Id} header
     * @param requisitionId the source requisition id
     * @return the existing converted order, or empty when this requisition has not
     *         been converted
     */
    Optional<PurchaseOrder> findByTenantIdAndCompanyIdAndSourcePurchaseRequisitionId(
            Long tenantId, Long companyId, Long requisitionId);

    /**
     * The highest numeric suffix already issued for a prefix in this scope.
     *
     * <p>Seeds a newly created period counter so a fiscal-year boundary continues the
     * series rather than reissuing a number the unique constraint on
     * {@code (tenant_id, company_id, po_number)} then rejects.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @param prefix    the document prefix, for example {@code PO-}
     * @param prefixLength the prefix length, used to strip it in the query
     * @return the highest issued value, or null when the scope has issued none
     */
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.poNumber, :prefixLength + 1) AS java.lang.Long)), 0) "
         + "FROM PurchaseOrder o "
         + "WHERE o.tenantId = :tenantId AND o.companyId = :companyId "
         + "AND o.poNumber LIKE CONCAT(:prefix, '%')")
    Long findHighestIssuedNumber(@Param("tenantId") Long tenantId,
                                 @Param("companyId") Long companyId,
                                 @Param("prefix") String prefix,
                                 @Param("prefixLength") int prefixLength);
}
