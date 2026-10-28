package com.erp.business.procurement.workflow.infrastructure.persistence;

import com.erp.business.procurement.workflow.domain.CompanyWorkflowConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyWorkflowConfigurationRepository
        extends JpaRepository<CompanyWorkflowConfiguration, Long> {

    /**
     * The company's workflow settings.
     *
     * <p>May legitimately return empty: a company that has never opened the
     * settings screen has no row, and the caller is expected to fall back to the
     * safe default rather than create one as a side effect of reading. Writing on a
     * read would mean a {@code GET} request changed the database.
     *
     * <p>The safe default is applied by the service, not by a {@code COALESCE}
     * here. A JPQL aggregate over an absent row yields no row rather than the
     * default value, so a query that tried to fold "unconfigured" into a result
     * would have to be a scalar count or a case expression over a left join -
     * considerably more machinery for one boolean, on a path that runs once per
     * company rather than once per request.
     *
     * @param tenantId  the owning tenant
     * @param companyId the owning company
     * @return the company's settings, or empty when never configured
     */
    Optional<CompanyWorkflowConfiguration> findByTenantIdAndCompanyId(Long tenantId, Long companyId);
}
