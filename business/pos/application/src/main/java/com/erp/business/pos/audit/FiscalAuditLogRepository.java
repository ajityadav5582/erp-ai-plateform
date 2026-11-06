package com.erp.business.pos.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface FiscalAuditLogRepository extends JpaRepository<FiscalAuditLog, Long> {

    @Query("SELECT l FROM FiscalAuditLog l WHERE l.companyId = :companyId " +
           "AND (:from IS NULL OR l.createdAt >= :from) " +
           "AND (:to IS NULL OR l.createdAt <= :to) " +
           "ORDER BY l.createdAt DESC")
    List<FiscalAuditLog> findByCompanyAndRange(@Param("companyId") Long companyId,
                                               @Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to,
                                               Pageable pageable);
}
