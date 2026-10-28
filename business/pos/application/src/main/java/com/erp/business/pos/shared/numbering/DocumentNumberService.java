package com.erp.business.pos.shared.numbering;

import com.erp.business.pos.shared.domain.DocumentSequence;
import com.erp.business.pos.shared.infrastructure.persistence.DocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates the next gap-free document number for a scope and period.
 *
 * <p>Runs in one transaction and takes a pessimistic write lock on the counter
 * row, so two concurrent requests cannot issue the same number. The caller must
 * persist the document inside the same transaction, keeping the allocated number
 * and the row that carries it atomic.
 */
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
