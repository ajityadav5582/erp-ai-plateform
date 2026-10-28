package com.erp.platform.shared.numbering;

import com.erp.platform.shared.domain.DocumentSequence;
import com.erp.platform.shared.infrastructure.persistence.DocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentNumberService {

    private static final int DEFAULT_WIDTH = 6;

    private final DocumentSequenceRepository sequenceRepository;

    public DocumentNumberService(DocumentSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String nextNumber(Long tenantId, Long companyId, String documentType,
                             String periodKey, String prefix) {
        return nextNumber(tenantId, companyId, documentType, periodKey, prefix, DEFAULT_WIDTH);
    }

    @Transactional
    public String nextNumber(Long tenantId, Long companyId, String documentType,
                             String periodKey, String prefix, int width) {
        DocumentSequence sequence = sequenceRepository
                .findForUpdate(tenantId, companyId, documentType, periodKey)
                .orElseGet(() -> sequenceRepository.save(
                        DocumentSequence.startAt(tenantId, companyId, documentType, periodKey)));
        long allocated = sequence.allocate();
        sequenceRepository.save(sequence);
        return sequence.format(allocated, prefix, width);
    }
}
