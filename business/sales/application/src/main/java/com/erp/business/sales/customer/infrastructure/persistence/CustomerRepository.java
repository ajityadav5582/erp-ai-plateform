package com.erp.business.sales.customer.infrastructure.persistence;

import com.erp.business.sales.customer.domain.Customer;
import com.erp.business.sales.customer.domain.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link Customer} aggregate.
 *
 * <p>Every finder is tenant- and company-scoped: the two ids are always the
 * leading parameters, so a query can never leak a row across a company boundary
 * even if a caller passes the wrong combination.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Customer> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<Customer> findByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    Optional<Customer> findByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    List<Customer> findByTenantIdAndCompanyId(Long tenantId, Long companyId);

    Page<Customer> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    List<Customer> findByTenantIdAndCompanyIdAndIsActiveTrue(Long tenantId, Long companyId);

    List<Customer> findByTenantIdAndCompanyIdAndType(Long tenantId, Long companyId, CustomerType type);

    boolean existsByTenantIdAndCompanyIdAndCode(Long tenantId, Long companyId, String code);

    boolean existsByTenantIdAndCompanyIdAndName(Long tenantId, Long companyId, String name);

    boolean existsByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.companyId = :companyId "
            + "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Customer> searchByName(@Param("tenantId") Long tenantId,
                                @Param("companyId") Long companyId,
                                @Param("name") String name,
                                Pageable pageable);
}
