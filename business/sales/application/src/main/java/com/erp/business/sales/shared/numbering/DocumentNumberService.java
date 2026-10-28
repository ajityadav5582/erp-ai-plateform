package com.erp.business.sales.shared.numbering;

import com.erp.business.sales.shared.domain.DocumentSequence;
import com.erp.business.sales.shared.infrastructure.persistence.DocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates the next human-readable document number for a scope and period.
 *
 * <p>The whole method runs in one transaction and takes a pessimistic write lock
 * on the counter row, so two concurrent requests cannot read the same value and
 * issue the same number. The caller must persist the document inside the same
 * transaction, which is what makes the allocated number and the row that carries
 * it atomic: either both commit or neither does, so the series stays gap-free.
 *
 * <p>This is the JPA-only replacement for a {@code next_document_number()} database
 * function; it needs no migration and works under {@code ddl-auto: update}.
 */
@Service
public class DocumentNumberService {

    /** Zero-padded width of the numeric part, e.g. {@code SO-000042}. */
    private static final int DEFAULT_WIDTH = 6;

    private final DocumentSequenceRepository sequenceRepository;

    public DocumentNumberService(DocumentSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Allocates and formats the next number for the given document family.
     *
     * @param tenantId     the owning tenant
     * @param companyId    the owning company
     * @param documentType the document family, e.g. {@code SALES_ORDER}
     * @param periodKey    the reset period, e.g. the fiscal-year label {@code 2081/82}
     * @param prefix       the printed prefix, e.g. {@code SO-}
     * @return the formatted document number, e.g. {@code SO-000042}
     */
    @Transactional
    public String nextNumber(Long tenantId, Long companyId, String documentType,
                             String periodKey, String prefix) {
        return nextNumber(tenantId, companyId, documentType, periodKey, prefix, DEFAULT_WIDTH);
    }

    /**
     * Allocates and formats the next number with an explicit zero-pad width.
     */
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
