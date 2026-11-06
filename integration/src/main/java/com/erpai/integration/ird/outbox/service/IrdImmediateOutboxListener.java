package com.erpai.integration.ird.outbox.service;

import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus;
import com.erpai.integration.ird.outbox.repository.IrdSyncOutboxRepository;
import com.erpai.integration.ird.port.out.CompanyIrdConfigRepositoryPort;
import com.erpai.integration.ird.port.out.IrdFiscalSyncPort;
import com.erpai.integration.ird.port.IrdSyncResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class IrdImmediateOutboxListener {

    @PersistenceContext
    private EntityManager entityManager;

    private final CompanyIrdConfigRepositoryPort companyIrdConfigRepositoryPort;
    private final IrdFiscalSyncPort irdFiscalSyncPort;
    private final IrdSyncOutboxRepository repository;

    public IrdImmediateOutboxListener(CompanyIrdConfigRepositoryPort companyIrdConfigRepositoryPort,
                                      IrdFiscalSyncPort irdFiscalSyncPort,
                                      IrdSyncOutboxRepository repository) {
        this.companyIrdConfigRepositoryPort = companyIrdConfigRepositoryPort;
        this.irdFiscalSyncPort = irdFiscalSyncPort;
        this.repository = repository;
    }

    @Async("irdVirtualThreadExecutor")
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFiscalBillCommitted(FiscalBillCommittedEvent event) {
        Optional<CompanyIrdConfig> configOpt =
                companyIrdConfigRepositoryPort.findByTenantIdAndCompanyId(event.tenantId(), event.companyId());

        if (configOpt.isEmpty() || !configOpt.get().isCbmsActive()) {
            repository.findById(event.outboxId()).ifPresent(entity -> {
                entity.setSyncStatus(IrdSyncOutboxStatus.SKIPPED);
                repository.save(entity);
            });
            return;
        }

        boolean isCreditNote = event.invoiceType() == com.erpai.integration.ird.outbox.entity.IrdInvoiceType.CREDIT_NOTE
                || event.creditNoteCommand() != null;

        try {
            IrdSyncResult result;
            if (isCreditNote) {
                result = irdFiscalSyncPort.syncCreditNote(event.creditNoteCommand().withRealtime(true));
            } else {
                result = irdFiscalSyncPort.syncSalesBill(event.billCommand().withRealtime(true));
            }
            int responseCode = (result != null) ? result.getStatusCode() : 0;

            if (result != null && result.isSuccess() && responseCode == 200) {
                if (result.isSimulated()) {
                    repository.markSimulated(event.outboxId(), responseCode, Instant.now());
                } else {
                    repository.markSynced(event.outboxId(), responseCode, Instant.now());
                }
                if (!isCreditNote) {
                    entityManager.createNativeQuery(
                                    "UPDATE sales_invoices SET cbms_synced = true, cbms_sync_time = ?1, cbms_retry_count = 0 WHERE id = ?2")
                            .setParameter(1, Instant.now())
                            .setParameter(2, event.sourceTransactionId())
                            .executeUpdate();
                    entityManager.createNativeQuery(
                                    "UPDATE pos_transactions SET cbms_synced = true, cbms_sync_time = ?1 WHERE id = (SELECT pos_transaction_id FROM sales_invoices WHERE id = ?2)")
                            .setParameter(1, Instant.now())
                            .setParameter(2, event.sourceTransactionId())
                            .executeUpdate();
                }
            } else {
                String errorMessage = (result != null) ? result.getErrorMessage() : "IRD sync failed";
                boolean alreadyRegistered = responseCode == 101
                        || (errorMessage != null && errorMessage.toLowerCase().contains("already registered"));
                if (alreadyRegistered) {
                    repository.markSynced(event.outboxId(), responseCode == 0 ? 101 : responseCode, Instant.now());
                    if (!isCreditNote) {
                        entityManager.createNativeQuery(
                                        "UPDATE pos_transactions SET cbms_synced = true, cbms_sync_time = ?1 WHERE id = ?2")
                                .setParameter(1, Instant.now())
                                .setParameter(2, event.sourceTransactionId())
                                .executeUpdate();
                    }
                } else {
                    IrdSyncOutboxEntity existing = repository.findById(event.outboxId()).orElse(null);
                    int retryCount = (existing != null) ? existing.getRetryCount() + 1 : 1;
                    Instant nextRetryAt = Instant.now().plusSeconds(60);
                    repository.markFailed(event.outboxId(), responseCode, errorMessage, retryCount, nextRetryAt);
                    if (!isCreditNote) {
                        entityManager.createNativeQuery(
                                        "UPDATE sales_invoices SET cbms_retry_count = cbms_retry_count + 1 WHERE id = ?1")
                                .setParameter(1, event.sourceTransactionId())
                                .executeUpdate();
                        entityManager.createNativeQuery(
                                        "UPDATE pos_transactions SET cbms_synced = false, cbms_retry_count = cbms_retry_count + 1 WHERE id = (SELECT pos_transaction_id FROM sales_invoices WHERE id = ?1)")
                                .setParameter(1, event.sourceTransactionId())
                                .executeUpdate();
                    }
                }
            }
        } catch (Exception ex) {
            IrdSyncOutboxEntity existing = repository.findById(event.outboxId()).orElse(null);
            int retryCount = (existing != null) ? existing.getRetryCount() + 1 : 1;
            Instant nextRetryAt = Instant.now().plusSeconds(60);
            repository.markFailed(event.outboxId(), 0, ex.getMessage(), retryCount, nextRetryAt);
                    if (!isCreditNote) {
                        entityManager.createNativeQuery(
                                        "UPDATE sales_invoices SET cbms_retry_count = cbms_retry_count + 1 WHERE id = ?1")
                                .setParameter(1, event.sourceTransactionId())
                                .executeUpdate();
                        entityManager.createNativeQuery(
                                        "UPDATE pos_transactions SET cbms_synced = false, cbms_retry_count = cbms_retry_count + 1 WHERE id = (SELECT pos_transaction_id FROM sales_invoices WHERE id = ?1)")
                                .setParameter(1, event.sourceTransactionId())
                                .executeUpdate();
                    }
        }
    }
}
