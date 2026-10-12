package com.erp.business.sales.salesorder.infrastructure.persistence;

import com.erp.business.sales.salesorder.domain.SalesOrder;
import com.erp.business.sales.salesorder.domain.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link SalesOrder} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary.
 */
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<SalesOrder> findByTenantIdAndCompanyIdAndSoNumber(Long tenantId, Long companyId, String soNumber);

    Page<SalesOrder> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesOrder> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                          SalesOrderStatus status, Pageable pageable);

    Page<SalesOrder> findByTenantIdAndCompanyIdAndCustomerId(Long tenantId, Long companyId,
                                                              Long customerId, Pageable pageable);

    List<SalesOrder> findByTenantIdAndCompanyIdAndStatusIn(Long tenantId, Long companyId,
                                                            List<SalesOrderStatus> statuses);

    boolean existsByTenantIdAndCompanyIdAndSoNumber(Long tenantId, Long companyId, String soNumber);

    @Query("SELECT o FROM SalesOrder o WHERE o.tenantId = :tenantId AND o.companyId = :companyId "
            + "AND LOWER(o.soNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<SalesOrder> searchByNumber(@Param("tenantId") Long tenantId,
                                    @Param("companyId") Long companyId,
                                    @Param("term") String term,
                                    Pageable pageable);
}
