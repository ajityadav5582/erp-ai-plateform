package com.erp.business.sales.quotation.infrastructure.persistence;

import com.erp.business.sales.quotation.domain.SalesQuotation;
import com.erp.business.sales.quotation.domain.SalesQuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesQuotationRepository extends JpaRepository<SalesQuotation, Long> {

    Optional<SalesQuotation> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<SalesQuotation> findByTenantIdAndCompanyIdAndQuotationNumber(Long tenantId, Long companyId, String quotationNumber);

    Page<SalesQuotation> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<SalesQuotation> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                            SalesQuotationStatus status, Pageable pageable);

    Page<SalesQuotation> findByTenantIdAndCompanyIdAndCustomerId(Long tenantId, Long companyId,
                                                                 Long customerId, Pageable pageable);

    List<SalesQuotation> findByTenantIdAndCompanyIdAndStatusIn(Long tenantId, Long companyId,
                                                               List<SalesQuotationStatus> statuses);

    boolean existsByTenantIdAndCompanyIdAndQuotationNumber(Long tenantId, Long companyId, String quotationNumber);

    @Query("SELECT q FROM SalesQuotation q WHERE q.tenantId = :tenantId AND q.companyId = :companyId "
            + "AND (LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<SalesQuotation> search(@Param("tenantId") Long tenantId,
                                @Param("companyId") Long companyId,
                                @Param("term") String term,
                                Pageable pageable);
}
