package com.erp.business.pos.receipt;

import com.erp.business.pos.audit.FiscalAuditService;
import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReprintService {

    private final PosTransactionRepository transactionRepository;
    private final FiscalAuditService auditService;

    public ReprintService(PosTransactionRepository transactionRepository, FiscalAuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReprintResponseDto reprint(Long transactionId, Long userId, String userName) {
        PosTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        int newCount = (tx.getPrintCount() == null ? 1 : tx.getPrintCount()) + 1;
        tx.setPrintCount(newCount);
        tx.setLastPrintedAt(Instant.now());
        tx.setLastPrintedBy(userId);
        transactionRepository.save(tx);

        auditService.logAction(tx.getTenantId(), tx.getCompanyId(), "INVOICE_REPRINTED",
                "PosTransaction", String.valueOf(tx.getId()), userId, userName, null, null);

        boolean isCopy = newCount > 1;
        int copyNumber = newCount - 1;
        return new ReprintResponseDto(isCopy, copyNumber, newCount);
    }
}
