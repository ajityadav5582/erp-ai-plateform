package com.erp.business.pos.register.infrastructure.persistence;

import com.erp.business.pos.domain.PosRegister;
import com.erp.business.pos.domain.PosRegisterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for the {@link PosRegister} aggregate. Every finder is
 * tenant- and company-scoped so a query can never leak across a company boundary.
 */
public interface PosRegisterRepository extends JpaRepository<PosRegister, Long> {

    Optional<PosRegister> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);

    Optional<PosRegister> findByTenantIdAndCompanyIdAndRegisterCode(Long tenantId, Long companyId, String registerCode);

    Page<PosRegister> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);

    Page<PosRegister> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId,
                                                          PosRegisterStatus status, Pageable pageable);

    List<PosRegister> findByTenantIdAndCompanyIdAndStatus(Long tenantId, Long companyId, PosRegisterStatus status);

    boolean existsByTenantIdAndCompanyIdAndRegisterCode(Long tenantId, Long companyId, String registerCode);

    @Query("SELECT r FROM PosRegister r WHERE r.tenantId = :tenantId AND r.companyId = :companyId "
            + "AND (LOWER(r.registerCode) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(r.registerName) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<PosRegister> search(@Param("tenantId") Long tenantId,
                             @Param("companyId") Long companyId,
                             @Param("term") String term,
                             Pageable pageable);
}
