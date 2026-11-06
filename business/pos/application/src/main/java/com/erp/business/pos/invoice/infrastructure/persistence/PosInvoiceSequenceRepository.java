package com.erp.business.pos.invoice.infrastructure.persistence;

import com.erp.business.pos.domain.PosInvoiceSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PosInvoiceSequenceRepository extends JpaRepository<PosInvoiceSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PosInvoiceSequence s WHERE s.tenantId = :tenantId AND s.companyId = :companyId AND s.fiscalYear = :fiscalYear")
    Optional<PosInvoiceSequence> findForUpdate(@Param("tenantId") Long tenantId,
                                              @Param("companyId") Long companyId,
                                              @Param("fiscalYear") String fiscalYear);
}
