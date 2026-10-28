package com.erp.business.pos.integration;

import com.erp.business.pos.domain.PosSalesIntegrationJob;
import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.integration.PosTransactionCompletedEvent;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PosSalesIntegrationWorker {

    private static final Logger log = LoggerFactory.getLogger(PosSalesIntegrationWorker.class);

    private final PosSalesIntegrationJobRepository repository;
    private final PosTransactionRepository transactionRepository;
    private final PosSalesEventPublisher publisher;

    public PosSalesIntegrationWorker(PosSalesIntegrationJobRepository repository,
                                    PosTransactionRepository transactionRepository,
                                    PosSalesEventPublisher publisher) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
        this.publisher = publisher;
    }

    private static final int MAX_ATTEMPTS = 5;
    private static final long LEASE_SECONDS = 60;

    @Scheduled(fixedDelayString = "${pos.integration.poll-interval-ms:5000}")
    @Transactional
    public void processPending() {
        List<PosSalesIntegrationJob> jobs = repository.findPendingForUpdate("PENDING", Instant.now());
        for (PosSalesIntegrationJob job : jobs) {
            if (job.getAttemptCount() >= MAX_ATTEMPTS) {
                job.markFailed("Max retry attempts exceeded");
                repository.save(job);
                log.error("POS sales integration job {} permanently failed", job.getId());
                continue;
            }
            Instant leaseExpiry = Instant.now().plusSeconds(LEASE_SECONDS);
            job.markInProgress(leaseExpiry);
            repository.save(job);

            try {
                PosTransaction tx = transactionRepository
                        .findByTenantIdAndCompanyIdAndId(job.getTenantId(), job.getCompanyId(), job.getPosTransactionId())
                        .orElseThrow(() -> new IllegalStateException("POS transaction not found: " + job.getPosTransactionId()));

                List<PosTransactionCompletedEvent.Line> eventLines = tx.getLines().stream()
                        .map(l -> new PosTransactionCompletedEvent.Line(
                                l.getItemId(), l.getUomCode(), l.getQuantity(), l.getUnitPrice(),
                                l.getDiscountPercentage(), l.getVatType().name(), l.getVatRate(), l.getDescription()))
                        .toList();
                PosTransactionCompletedEvent event = new PosTransactionCompletedEvent(
                        UUID.randomUUID(),
                        String.valueOf(job.getTenantId()),
                        job.getCompanyId(),
                        tx.getFiscalYearId(),
                        tx.getId(),
                        tx.getTransactionNumber(),
                        tx.getCustomerId(),
                        tx.getCustomerName(),
                        tx.getCustomerPanNumber(),
                        tx.getTransactionDate(),
                        tx.getNotes(),
                        tx.getCashierId(),
                        eventLines);
                publisher.publish(event);
                job.markCompleted();
                job.setLeaseExpiresAt(null);
                repository.save(job);
            } catch (Exception ex) {
                long backoff = Math.min(3600, 60L * (1L << Math.min(job.getAttemptCount(), 6)));
                Instant nextRetry = Instant.now().plusSeconds(backoff);
                job.markPending(ex.getMessage(), nextRetry);
                job.setLeaseExpiresAt(null);
                repository.save(job);
                log.warn("POS sales integration job {} failed: {}", job.getId(), ex.getMessage());
            }
        }
    }
}
