package com.erp.business.pos.cbms;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.pos.domain.PosTransactionStatus;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Outbox worker that pushes completed POS sales to Nepal IRD's Central Billing
 * Monitoring System (CBMS).
 *
 * <p><strong>Off the synchronous path.</strong> Checkout never calls CBMS: the
 * sale commits first and the worker drains the outbox afterwards, so a slow or
 * unreachable IRD endpoint can never delay a customer at the counter or fail a
 * completed sale.
 *
 * <p><strong>Bounded retries.</strong> A failed push increments the
 * transaction's {@code cbmsRetryCount}; once it reaches the configured maximum
 * the transaction is abandoned by the worker (and logged) rather than retried
 * forever, so one poison bill cannot clog the outbox.
 */
@Service
@Slf4j
public class PosCbmsSyncWorker {

    private final PosTransactionRepository transactionRepository;
    private final PosCbmsClient cbmsClient;

    @Value("${ird.cbms.seller-pan:123456789}")
    private String sellerPan;

    @Value("${ird.cbms.max-retries:5}")
    private int maxRetries;

    public PosCbmsSyncWorker(PosTransactionRepository transactionRepository, PosCbmsClient cbmsClient) {
        this.transactionRepository = transactionRepository;
        this.cbmsClient = cbmsClient;
    }

    /**
     * Outbox scheduler: runs every 30 seconds to push un-synced completed
     * sales to CBMS.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processOutbox() {
        List<PosTransaction> pending = transactionRepository
                .findByCbmsSyncedFalseAndStatus(PosTransactionStatus.COMPLETED);

        if (pending.isEmpty()) {
            return;
        }

        int pushed = 0;
        int abandoned = 0;
        for (PosTransaction transaction : pending) {
            if (transaction.getCbmsRetryCount() >= maxRetries) {
                abandoned++;
                continue;
            }
            pushed++;
            syncTransaction(transaction);
        }

        log.info("CBMS Outbox: attempted {} POS transaction(s), {} abandoned after {} failed attempts",
                pushed, abandoned, maxRetries);
    }

    /**
     * Pushes one transaction and records the outcome on the aggregate. Exposed
     * so the controller can trigger a manual retry.
     */
    @Transactional
    public PosCbmsSyncResult syncTransaction(PosTransaction transaction) {
        PosCbmsBillPayload payload = buildPayload(transaction);

        PosCbmsSyncResult result = cbmsClient.postBill(payload);
        if (result.success()) {
            transaction.markCbmsSynced(result.responseBody());
        } else {
            transaction.recordCbmsSyncFailure(result.message());
        }
        transactionRepository.save(transaction);
        return result;
    }

    private PosCbmsBillPayload buildPayload(PosTransaction transaction) {
        return new PosCbmsBillPayload(
                cbmsClient.getUsername(),
                cbmsClient.getPassword(),
                sellerPan,
                transaction.getCustomerPanNumber() != null ? transaction.getCustomerPanNumber() : "",
                transaction.getCustomerName(),
                fiscalYearLabel(transaction),
                transaction.getTransactionNumber(),
                transaction.getTransactionDateNp() != null
                        ? transaction.getTransactionDateNp() : transaction.getTransactionDate().toString(),
                transaction.getTotalAmount(),
                discountTotal(transaction),
                transaction.getTaxableAmount(),
                transaction.getVatAmount(),
                transaction.getExemptAmount(),
                true
        );
    }

    /**
     * Total line discount: the difference between each line's gross
     * ({@code quantity × unitPrice}) and its net amount, summed. Computed from
     * the frozen line snapshots so the reported figure always matches the sale.
     */
    private BigDecimal discountTotal(PosTransaction transaction) {
        BigDecimal discount = BigDecimal.ZERO;
        for (PosTransactionLine line : transaction.getLines()) {
            BigDecimal gross = line.getQuantity().multiply(line.getUnitPrice());
            discount = discount.add(gross.subtract(line.getLineAmount()));
        }
        return discount.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Derives the Nepali fiscal-year label (e.g. {@code "2081/82"}) from the
     * transaction's BS date. The fiscal year runs 1 Shrawan (BS month 4) to
     * the end of Ashadh, so a date before Shrawan belongs to the year that
     * began the previous Shrawan.
     */
    private String fiscalYearLabel(PosTransaction transaction) {
        String bsDate = transaction.getTransactionDateNp();
        if (bsDate == null || bsDate.length() < 7) {
            return String.valueOf(transaction.getTransactionDate().getYear());
        }
        try {
            int bsYear = Integer.parseInt(bsDate.substring(0, 4));
            int bsMonth = Integer.parseInt(bsDate.substring(5, 7));
            int startYear = bsMonth >= 4 ? bsYear : bsYear - 1;
            return startYear + "/" + String.format("%02d", (startYear + 1) % 100);
        } catch (RuntimeException ex) {
            log.warn("Could not derive fiscal year from BS date '{}' for transaction {}",
                    bsDate, transaction.getTransactionNumber());
            return String.valueOf(transaction.getTransactionDate().getYear());
        }
    }
}
