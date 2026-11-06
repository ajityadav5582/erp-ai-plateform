package com.erp.business.sales.integration;

import com.erp.business.pos.domain.PosSalesIntegrationJob;
import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.sales.invoice.SalesInvoiceService;
import com.erp.business.sales.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceLineRequest;
import com.erp.business.sales.invoice.domain.VatType;
import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PosSalesIntegrationWorker {

    private final PosSalesIntegrationJobRepository jobRepository;
    private final PosTransactionRepository transactionRepository;
    private final SalesInvoiceService salesInvoiceService;

    public PosSalesIntegrationWorker(PosSalesIntegrationJobRepository jobRepository,
                                    PosTransactionRepository transactionRepository,
                                    SalesInvoiceService salesInvoiceService) {
        this.jobRepository = jobRepository;
        this.transactionRepository = transactionRepository;
        this.salesInvoiceService = salesInvoiceService;
    }

    private static final int MAX_ATTEMPTS = 5;
    private static final long LEASE_SECONDS = 60;

    @Scheduled(fixedDelayString = "${pos.integration.poll-interval-ms:5000}")
    @Transactional
    public void processPending() {
        List<PosSalesIntegrationJob> jobs = jobRepository.findPendingForUpdate("PENDING", Instant.now());
        for (PosSalesIntegrationJob job : jobs) {
            if (job.getAttemptCount() >= MAX_ATTEMPTS) {
                job.markFailed("Max retry attempts exceeded");
                jobRepository.save(job);
                log.error("POS sales integration job {} permanently failed", job.getId());
                continue;
            }
            Instant leaseExpiry = Instant.now().plusSeconds(LEASE_SECONDS);
            job.markInProgress(leaseExpiry);
            jobRepository.save(job);

            try {
                PosTransaction tx = transactionRepository
                        .findByTenantIdAndCompanyIdAndId(job.getTenantId(), job.getCompanyId(), job.getPosTransactionId())
                        .orElseThrow(() -> new IllegalStateException("POS transaction not found: " + job.getPosTransactionId()));

                TenantContext.setTenantId(String.valueOf(job.getTenantId()));
                RequestContext.setTenantId(String.valueOf(job.getTenantId()));
                RequestContext.setCompanyId(String.valueOf(job.getCompanyId()));
                if (tx.getFiscalYearId() != null) {
                    RequestContext.setFiscalYearId(String.valueOf(tx.getFiscalYearId()));
                }
                if (tx.getCashierId() != null) {
                    RequestContext.setUserId(tx.getCashierId());
                }

                List<InvoiceLineRequest> lines = tx.getLines().stream()
                        .map(l -> new InvoiceLineRequest(
                                l.getItemId(),
                                l.getUomCode(),
                                l.getQuantity(),
                                l.getUnitPrice(),
                                l.getDiscountPercentage(),
                                VatType.valueOf(l.getVatType().name()),
                                l.getVatRate(),
                                l.getDescription()))
                        .toList();

                CreatePosInvoiceRequest request = new CreatePosInvoiceRequest(
                        tx.getId(),
                        tx.getCustomerId(),
                        tx.getCustomerName(),
                        tx.getCustomerPanNumber(),
                        tx.getTransactionDate(),
                        tx.getNotes(),
                        lines);

                salesInvoiceService.createPosInvoice(job.getTenantId(), job.getCompanyId(), request);

                job.markCompleted();
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);
            } catch (Exception ex) {
                long backoff = Math.min(3600, 60L * (1L << Math.min(job.getAttemptCount(), 6)));
                Instant nextRetry = Instant.now().plusSeconds(backoff);
                job.markPending(ex.getMessage(), nextRetry);
                job.setLeaseExpiresAt(null);
                jobRepository.save(job);
                log.warn("POS sales integration job {} failed: {}", job.getId(), ex.getMessage());
            } finally {
                TenantContext.clear();
                RequestContext.clear();
            }
        }
    }
}
