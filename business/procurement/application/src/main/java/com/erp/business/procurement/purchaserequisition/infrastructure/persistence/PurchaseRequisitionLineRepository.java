package com.erp.business.procurement.purchaserequisition.infrastructure.persistence;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLine;
import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequisitionLineRepository
        extends JpaRepository<PurchaseRequisitionLine, Long> {

    Optional<PurchaseRequisitionLine> findByTenantIdAndCompanyIdAndId(
            Long tenantId, Long companyId, Long id);

    List<PurchaseRequisitionLine> findByRequisitionIdOrderByLineNumberAsc(Long requisitionId);

    List<PurchaseRequisitionLine> findByTenantIdAndCompanyIdAndRequisitionIdOrderByLineNumberAsc(
            Long tenantId, Long companyId, Long requisitionId);

    List<PurchaseRequisitionLine> findByTenantIdAndCompanyIdAndItemIdAndStatusIn(
            Long tenantId, Long companyId, Long itemId, Collection<PurchaseRequisitionLineStatus> statuses);

    /**
     * Procurement consolidation view: every still-orderable line for an item across
     * all branches of this company, so a buyer can pool demand onto one order.
     */
    @Query("SELECT l FROM PurchaseRequisitionLine l WHERE l.tenantId = :tenantId " +
           "AND l.companyId = :companyId AND l.itemId = :itemId AND l.status IN :statuses " +
           "ORDER BY l.requisition.branchId ASC, l.lineNumber ASC")
    List<PurchaseRequisitionLine> findOrderableDemandForItem(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("itemId") Long itemId,
            @Param("statuses") Collection<PurchaseRequisitionLineStatus> statuses);

    /**
     * Un-ordered approved quantity, used for the buyer worklist.
     */
    @Query("SELECT l FROM PurchaseRequisitionLine l WHERE l.tenantId = :tenantId " +
           "AND l.companyId = :companyId AND l.status IN :statuses " +
           "AND l.orderedQuantity < l.approvedQuantity")
    List<PurchaseRequisitionLine> findOutstandingDemand(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<PurchaseRequisitionLineStatus> statuses);
}
