package com.erp.business.procurement.purchaserequisition.infrastructure.persistence;

import com.erp.business.procurement.purchaserequisition.domain.PurchaseRequisitionOrderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseRequisitionOrderLinkRepository
        extends JpaRepository<PurchaseRequisitionOrderLink, Long> {

    List<PurchaseRequisitionOrderLink> findByRequisitionIdOrderByLinkedAtAsc(Long requisitionId);

    List<PurchaseRequisitionOrderLink> findByTenantIdAndCompanyIdAndPurchaseOrderId(
            Long tenantId, Long companyId, Long purchaseOrderId);

    List<PurchaseRequisitionOrderLink> findByPurchaseRequisitionLineIdOrderByLinkedAtAsc(
            Long purchaseRequisitionLineId);

    /**
     * Total already awarded on a line. Must equal the line's orderedQuantity;
     * a mismatch means a link was written without updating the line.
     */
    @Query("SELECT COALESCE(SUM(l.orderedQuantity), 0) FROM PurchaseRequisitionOrderLink l " +
           "WHERE l.purchaseRequisitionLineId = :lineId")
    java.math.BigDecimal sumOrderedQuantityForLine(
            @Param("lineId") Long lineId);
}
