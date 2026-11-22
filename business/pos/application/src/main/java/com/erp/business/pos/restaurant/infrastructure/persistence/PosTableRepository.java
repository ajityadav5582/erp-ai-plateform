package com.erp.business.pos.restaurant.infrastructure.persistence;

import com.erp.business.pos.restaurant.domain.PosTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosTableRepository extends JpaRepository<PosTable, Long> {
    Optional<PosTable> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
    List<PosTable> findByTenantIdAndCompanyId(Long tenantId, Long companyId);
    boolean existsByTenantIdAndCompanyIdAndTableNumber(Long tenantId, Long companyId, String tableNumber);
    boolean existsByTenantIdAndCompanyIdAndTableNumberAndIdNot(Long tenantId, Long companyId, String tableNumber, Long id);
    long countByTenantIdAndCompanyId(Long tenantId, Long companyId);
}
