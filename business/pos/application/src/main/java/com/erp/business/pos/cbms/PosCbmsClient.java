package com.erp.business.pos.cbms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for Nepal IRD's Central Billing Monitoring System (CBMS).
 *
 * <p>Mirrors the Sales module's client so both modules push the same payload
 * shape to the same endpoint. When CBMS is disabled or unconfigured (the
 * default in development), pushes are simulated as successful so the rest of
 * the flow — marking the transaction synced, the outbox draining — can be
 * exercised end to end without IRD credentials.
 */
@Component
@Slf4j
public class PosCbmsClient {

    @Value("${ird.cbms.url:https://cbms.ird.gov.np/api}")
    private String cbmsUrl;

    @Value("${ird.cbms.username:}")
    private String defaultUsername;

    @Value("${ird.cbms.password:}")
    private String defaultPassword;

    @Value("${ird.cbms.enabled:false}")
    private boolean enabled;

    private final RestClient restClient;

    public PosCbmsClient() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Pushes one POS bill to CBMS. Never throws: a failure is returned as a
     * failed result so the caller can record it against the transaction and
     * let the bounded-retry outbox try again.
     */
    public PosCbmsSyncResult postBill(PosCbmsBillPayload payload) {
        if (!enabled || defaultUsername.isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for bill: {}",
                    payload.bill_no());
            return PosCbmsSyncResult.ok("Simulated 200 OK: IRD CBMS Bill recorded successfully");
        }

        try {
            String response = restClient.post()
                    .uri(cbmsUrl + "/bill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("CBMS Bill push succeeded for bill {}: {}", payload.bill_no(), response);
            return PosCbmsSyncResult.ok(response != null ? response : "200 OK");
        } catch (Exception ex) {
            log.error("CBMS Bill push failed for bill {}: {}", payload.bill_no(), ex.getMessage());
            return PosCbmsSyncResult.fail(500, ex.getMessage());
        }
    }

    public String getUsername() {
        return defaultUsername;
    }

    public String getPassword() {
        return defaultPassword;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
