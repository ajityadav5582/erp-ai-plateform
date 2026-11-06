package com.erp.business.pos.restaurant.infrastructure.persistence;

import com.erp.business.pos.restaurant.domain.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    List<TableSession> findByTenantIdAndCompanyIdAndTableId(Long tenantId, Long companyId, Long tableId);
    Optional<TableSession> findByTenantIdAndCompanyIdAndId(Long tenantId, Long companyId, Long id);
}
