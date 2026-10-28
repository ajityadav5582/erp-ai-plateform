package com.erp.business.pos.table;

import com.erp.business.pos.domain.PosTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosTableRepository extends JpaRepository<PosTable, Long> {
    Optional<PosTable> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
    List<PosTable> findByTenantIdAndCompanyId(Long tenantId, Long companyId);
    Page<PosTable> findByTenantIdAndCompanyId(Long tenantId, Long companyId, Pageable pageable);
    boolean existsByTenantIdAndCompanyIdAndTableName(Long tenantId, Long companyId, String tableName);
}
