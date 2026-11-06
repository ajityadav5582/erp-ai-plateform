package com.erp.business.pos.service.sequence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalBillSequenceRepository extends JpaRepository<FiscalBillSequenceEntity, Long> {

    @Query(value = """
            INSERT INTO fiscal_bill_sequences (tenant_id, company_id, fiscal_year_id, document_type, prefix, current_value, updated_at)
            VALUES (:tenantId, :companyId, :fiscalYearId, :docType, :prefix, 1, NOW())
            ON CONFLICT (tenant_id, company_id, fiscal_year_id, document_type)
            DO UPDATE SET current_value = fiscal_bill_sequences.current_value + 1, updated_at = NOW()
            RETURNING prefix || LPAD(current_value::text, 7, '0')
            """, nativeQuery = true)
    String allocateNext(@Param("tenantId") Long tenantId,
                        @Param("companyId") Long companyId,
                        @Param("fiscalYearId") Long fiscalYearId,
                        @Param("docType") String docType,
                        @Param("prefix") String prefix);
}
