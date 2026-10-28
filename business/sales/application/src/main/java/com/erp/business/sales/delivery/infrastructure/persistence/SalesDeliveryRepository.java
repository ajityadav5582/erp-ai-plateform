package com.erp.business.sales.delivery.infrastructure.persistence;

import com.erp.business.sales.delivery.domain.DeliveryStatus;
import com.erp.business.sales.delivery.domain.SalesDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link SalesDelivery} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary.
 */
public interface SalesDeliveryRepository extends JpaRepository<SalesDelivery, Long> {

    Optional<SalesDelivery> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<SalesDelivery> findByTenantIdAndCompanyIdAndDeliveryNumber(
            Long tenantId, Long companyId, String deliveryNumber);

    Page<SalesDelivery> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesDelivery> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                             DeliveryStatus status, Pageable pageable);

    Page<SalesDelivery> findByTenantIdAndCompanyIdAndSalesOrderId(Long tenantId, Long companyId,
                                                                   Long salesOrderId, Pageable pageable);

    List<SalesDelivery> findByTenantIdAndCompanyIdAndSalesOrderIdAndStatus(
            Long tenantId, Long companyId, Long salesOrderId, DeliveryStatus status);

    boolean existsByTenantIdAndCompanyIdAndDeliveryNumber(Long tenantId, Long companyId, String deliveryNumber);

    @Query("SELECT d FROM SalesDelivery d WHERE d.tenantId = :tenantId AND d.companyId = :companyId "
            + "AND LOWER(d.deliveryNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<SalesDelivery> searchByNumber(@Param("tenantId") Long tenantId,
                                       @Param("companyId") Long companyId,
                                       @Param("term") String term,
                                       Pageable pageable);
}
