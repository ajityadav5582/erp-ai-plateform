package com.erp.business.pos.service.sequence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FiscalSequenceService {

    private final FiscalBillSequenceRepository repository;

    public FiscalSequenceService(FiscalBillSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String nextNumber(Long tenantId, Long companyId, Long fiscalYearId,
                             String documentType, String prefix) {
        return repository.allocateNext(tenantId, companyId, fiscalYearId, documentType, prefix);
    }
}
