package com.erp.business.pos.cbms;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;

@Service
public class CbmsSyncService {

    private final WebClient webClient;

    public CbmsSyncService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://cbapi.ird.gov.np/api").build();
    }

    @Transactional
    public void syncTransaction(Long transactionId, boolean realtime) {
        // Placeholder: load transaction, build payload, call IRD, update tracking columns.
        // Implementation depends on PosTransaction entity and repository.
    }

    @Scheduled(fixedDelay = 120000)
    public void retryPending() {
        // Query unsynced transactions and re-attempt with isrealtime=false.
    }
}
