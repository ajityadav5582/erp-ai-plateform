package com.erp.business.procurement.purchasereturn.infrastructure.persistence;

import com.erp.business.procurement.purchasereturn.domain.PurchaseReturnLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PurchaseReturnLineRepository extends JpaRepository<PurchaseReturnLine, Long> {

    List<PurchaseReturnLine> findByPurchaseReturnIdOrderByLineNumberAsc(Long purchaseReturnId);

    @Query("SELECT COALESCE(SUM(l.returnQuantity), 0) FROM PurchaseReturnLine l "
         + "JOIN l.purchaseReturn r "
         + "WHERE l.tenantId = :tenantId AND l.companyId = :companyId "
         + "AND l.purchaseInvoiceLineId = :invoiceLineId AND r.status <> 'CANCELLED'")
    BigDecimal sumReturnedQuantityForInvoiceLine(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("invoiceLineId") Long invoiceLineId);

    @Query("SELECT l.purchaseInvoiceLineId, COALESCE(SUM(l.returnQuantity), 0) FROM PurchaseReturnLine l "
         + "JOIN l.purchaseReturn r "
         + "WHERE l.tenantId = :tenantId AND l.companyId = :companyId "
         + "AND r.purchaseInvoiceId = :invoiceId AND r.status <> 'CANCELLED' "
         + "GROUP BY l.purchaseInvoiceLineId")
    List<Object[]> sumReturnedQuantitiesByInvoiceId(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("invoiceId") Long invoiceId);
}

