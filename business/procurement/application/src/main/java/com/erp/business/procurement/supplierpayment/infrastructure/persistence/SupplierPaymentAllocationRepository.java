package com.erp.business.procurement.supplierpayment.infrastructure.persistence;

import com.erp.business.procurement.supplierpayment.domain.SupplierPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Persistence access to supplier payment allocations.
 */
public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocation, Long> {

    @Query("SELECT a FROM SupplierPaymentAllocation a WHERE a.tenantId = :tenantId "
         + "AND a.companyId = :companyId AND a.payment.id = :paymentId")
    List<SupplierPaymentAllocation> findByPaymentId(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("paymentId") Long paymentId);

    @Query("SELECT a FROM SupplierPaymentAllocation a WHERE a.tenantId = :tenantId "
         + "AND a.companyId = :companyId AND a.purchaseInvoiceId = :purchaseInvoiceId")
    List<SupplierPaymentAllocation> findByPurchaseInvoiceId(
            @Param("tenantId") Long tenantId,
            @Param("companyId") Long companyId,
            @Param("purchaseInvoiceId") Long purchaseInvoiceId);
}

