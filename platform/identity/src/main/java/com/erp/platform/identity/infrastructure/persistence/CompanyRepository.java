package com.erp.platform.identity.infrastructure.persistence;

import com.erp.platform.identity.domain.Company;
import com.erp.platform.identity.domain.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Company aggregate.
 *
 * <p>Provides data access methods for company management operations.
 * All queries are tenant-scoped to ensure data isolation.
 *
 * @since 1.0.0
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * Finds a company by tenant ID and its identifier.
     *
     * @param tenantId the tenant ID
     * @param entityId the company identifier
     * @return the company if found, empty otherwise
     */
    Optional<Company> findByTenantIdAndId(Long tenantId, Long entityId);

    /**
     * Finds a company by tenant ID and company code.
     *
     * <p>Used for lookups during company creation and validation.
     *
     * @param tenantId the tenant ID
     * @param companyCode the company code
     * @return the company if found, empty otherwise
     */
    Optional<Company> findByTenantIdAndCompanyCode(Long tenantId, String companyCode);

    /**
     * Finds a company by tenant ID and company name.
     *
     * <p>Used for lookups during company creation and validation.
     *
     * @param tenantId the tenant ID
     * @param companyName the company name
     * @return the company if found, empty otherwise
     */
    Optional<Company> findByTenantIdAndCompanyName(Long tenantId, String companyName);

    /**
     * Finds all companies for a specific tenant.
     *
     * @param tenantId the tenant ID
     * @return list of companies for the tenant
     */
    List<Company> findByTenantId(Long tenantId);

    /**
     * Finds all companies for a specific tenant, paginated.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination parameters
     * @return a page of companies for the tenant
     */
    Page<Company> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds all active companies for a specific tenant.
     *
     * <p>Used for operational queries and dropdown selections.
     *
     * @param tenantId the tenant ID
     * @return list of active companies for the tenant
     */
    List<Company> findByTenantIdAndStatus(Long tenantId, CompanyStatus status);

    /**
     * Checks if a company code exists within a tenant.
     *
     * @param tenantId the tenant ID
     * @param companyCode the company code to check
     * @return true if the company code exists, false otherwise
     */
    boolean existsByTenantIdAndCompanyCode(Long tenantId, String companyCode);

    /**
     * Checks if a company name exists within a tenant.
     *
     * @param tenantId the tenant ID
     * @param companyName the company name to check
     * @return true if the company name exists, false otherwise
     */
    boolean existsByTenantIdAndCompanyName(Long tenantId, String companyName);

    boolean existsByTenantIdAndCompanyNameIgnoreCase(Long tenantId, String companyName);

    boolean existsByTenantIdAndPanNumberIgnoreCase(Long tenantId, String panNumber);

    /**
     * Counts the number of companies for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the count of companies
     */
    long countByTenantId(Long tenantId);

    /**
     * Counts the number of active companies for a tenant.
     *
     * @param tenantId the tenant ID
     * @return the count of active companies
     */
    long countByTenantIdAndStatus(Long tenantId, CompanyStatus status);

    /**
     * Finds companies by tenant and status, ordered by company name.
     *
     * @param tenantId the tenant ID
     * @param status the company status
     * @return list of companies with the specified status
     */
    List<Company> findByTenantIdAndStatusOrderByCompanyName(Long tenantId, CompanyStatus status);
}
