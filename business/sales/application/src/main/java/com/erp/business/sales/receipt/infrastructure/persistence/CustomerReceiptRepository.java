package com.erp.business.sales.receipt.infrastructure.persistence;

import com.erp.business.sales.receipt.domain.CustomerReceipt;
import com.erp.business.sales.receipt.domain.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link CustomerReceipt} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary.
 */
public interface CustomerReceiptRepository extends JpaRepository<CustomerReceipt, Long> {

    Optional<CustomerReceipt> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<CustomerReceipt> findByTenantIdAndCompanyIdAndReceiptNumber(
            Long tenantId, Long companyId, String receiptNumber);

    Page<CustomerReceipt> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<CustomerReceipt> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                               ReceiptStatus status, Pageable pageable);

    Page<CustomerReceipt> findByTenantIdAndCompanyIdAndInvoiceId(Long tenantId, Long companyId,
                                                                  Long invoiceId, Pageable pageable);

    Page<CustomerReceipt> findByTenantIdAndCompanyIdAndCustomerId(Long tenantId, Long companyId,
                                                                   Long customerId, Pageable pageable);

    List<CustomerReceipt> findByTenantIdAndCompanyIdAndInvoiceIdAndStatus(
            Long tenantId, Long companyId, Long invoiceId, ReceiptStatus status);

    boolean existsByTenantIdAndCompanyIdAndReceiptNumber(Long tenantId, Long companyId, String receiptNumber);

    @Query("SELECT r FROM CustomerReceipt r WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
            + "AND LOWER(r.receiptNumber) LIKE LOWER(CONCAT('%', :term, '%'))")
    Page<CustomerReceipt> searchByNumber(@Param("tenantId") Long tenantId,
                                         @Param("companyId") Long companyId,
                                         @Param("term") String term,
                                         Pageable pageable);
}
