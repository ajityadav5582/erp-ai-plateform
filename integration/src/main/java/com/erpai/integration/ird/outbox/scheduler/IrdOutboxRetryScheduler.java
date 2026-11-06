package com.erpai.integration.ird.outbox.scheduler;

import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.repository.IrdSyncOutboxRepository;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.erpai.integration.ird.port.out.IrdFiscalSyncPort;
import com.erpai.integration.ird.port.IrdSyncResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class IrdOutboxRetryScheduler {

    @PersistenceContext
    private EntityManager entityManager;

    private final IrdSyncOutboxRepository outboxRepository;
    private final IrdFiscalSyncPort irdFiscalSyncPort;
    private final ObjectMapper objectMapper;

    public IrdOutboxRetryScheduler(IrdSyncOutboxRepository outboxRepository,
                                  IrdFiscalSyncPort irdFiscalSyncPort,
                                  ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.irdFiscalSyncPort = irdFiscalSyncPort;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${erpai.ird.outbox.poll-interval-ms:60000}")
    @Transactional
    public void retryPendingOutboxRecords() {
        List<IrdSyncOutboxEntity> records = outboxRepository.fetchPendingRecordsForUpdate(Instant.now(), 50);

        for (IrdSyncOutboxEntity record : records) {
            try {
                boolean isCreditNote = record.getInvoiceType() == com.erpai.integration.ird.outbox.entity.IrdInvoiceType.CREDIT_NOTE;

                IrdSyncResult result;
                if (isCreditNote) {
                    FiscalCreditNoteCommand command = objectMapper.readValue(record.getPayloadJson(), FiscalCreditNoteCommand.class);
                    command = command.withRealtime(false);
                    result = irdFiscalSyncPort.syncCreditNote(command);
                } else {
                    FiscalBillCommand command = objectMapper.readValue(record.getPayloadJson(), FiscalBillCommand.class);
                    command = command.withRealtime(false);
                    result = irdFiscalSyncPort.syncSalesBill(command);
                }
                int responseCode = (result != null) ? result.getStatusCode() : 0;

                boolean alreadyRegistered = responseCode == 101
                        || (result != null && result.getErrorMessage() != null
                        && result.getErrorMessage().toLowerCase().contains("already registered"));
                if ((result != null && result.isSuccess() && responseCode == 200) || alreadyRegistered) {
                    int code = alreadyRegistered ? (responseCode == 0 ? 101 : responseCode) : responseCode;
                    outboxRepository.markSynced(record.getId(), code, Instant.now());
                    if (record.getInvoiceType() != com.erpai.integration.ird.outbox.entity.IrdInvoiceType.CREDIT_NOTE) {
                        entityManager.createNativeQuery(
                                        "UPDATE pos_transactions SET cbms_synced = true, cbms_sync_time = ?1 WHERE id = ?2")
                                .setParameter(1, Instant.now())
                                .setParameter(2, record.getSourceTransactionId())
                                .executeUpdate();
                    }
                } else {
                    String errorMessage = (result != null) ? result.getErrorMessage() : "IRD sync failed";
                    int newRetryCount = record.getRetryCount() + 1;

                    if (newRetryCount >= record.getMaxRetries()) {
                        outboxRepository.markDeadLetter(record.getId(), responseCode, errorMessage);
                        log.error("[IRD-ALERT] Invoice {} reached max retries. Manual auditor review required.",
                                record.getInvoiceNumber());
                    } else {
                        int delaySeconds = Math.min(3600, (int) Math.pow(2, newRetryCount) * 60);
                        Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);
                        outboxRepository.markFailed(record.getId(), responseCode, errorMessage, newRetryCount, nextRetryAt);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to process outbox record {}", record.getId(), ex);
                int newRetryCount = record.getRetryCount() + 1;
                if (newRetryCount >= record.getMaxRetries()) {
                    outboxRepository.markDeadLetter(record.getId(), 0, ex.getMessage());
                    log.error("[IRD-ALERT] Invoice {} reached max retries. Manual auditor review required.",
                            record.getInvoiceNumber());
                } else {
                    int delaySeconds = Math.min(3600, (int) Math.pow(2, newRetryCount) * 60);
                    Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);
                    outboxRepository.markFailed(record.getId(), 0, ex.getMessage(), newRetryCount, nextRetryAt);
                }
            }
        }
    }
}
