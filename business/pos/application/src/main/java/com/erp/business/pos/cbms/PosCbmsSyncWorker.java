package com.erp.business.pos.cbms;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retired: POS sales are now reported to IRD exclusively through the central
 * IRD outbox (via the Sales invoice created from the POS transaction). This
 * worker no longer submits bills independently, to avoid duplicate reporting.
 */
@Service
@Slf4j
public class PosCbmsSyncWorker {

    private final PosTransactionRepository transactionRepository;

    public PosCbmsSyncWorker(PosTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PosCbmsSyncResult syncTransaction(PosTransaction transaction) {
        log.info("POS CBMS sync retired; transaction {} is handled by the central IRD outbox.",
                transaction.getTransactionNumber());
        return PosCbmsSyncResult.ok("Handled by central IRD outbox");
    }
}
