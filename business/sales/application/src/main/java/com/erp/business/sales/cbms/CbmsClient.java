package com.erp.business.sales.cbms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class CbmsClient {

    @Value("${ird.cbms.url:https://cbms.ird.gov.np/api}")
    private String cbmsUrl;

    @Value("${ird.cbms.username:}")
    private String defaultUsername;

    @Value("${ird.cbms.password:}")
    private String defaultPassword;

    @Value("${ird.cbms.enabled:false}")
    private boolean enabled;

    private final RestClient restClient;

    public CbmsClient() {
        this.restClient = RestClient.builder().build();
    }

    public CbmsSyncResult postBill(CbmsBillPayload payload) {
        if (!enabled || defaultUsername.isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for bill: {}", payload.bill_no());
            return CbmsSyncResult.ok("Simulated 200 OK: IRD CBMS Bill recorded successfully");
        }

        try {
            String response = restClient.post()
                    .uri(cbmsUrl + "/bill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("CBMS Bill push succeeded for bill {}: {}", payload.bill_no(), response);
            return CbmsSyncResult.ok(response != null ? response : "200 OK");
        } catch (Exception ex) {
            log.error("CBMS Bill push failed for bill {}: {}", payload.bill_no(), ex.getMessage());
            return CbmsSyncResult.fail(500, ex.getMessage());
        }
    }

    public CbmsSyncResult postBillReturn(CbmsBillReturnPayload payload) {
        if (!enabled || defaultUsername.isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for return: {}", payload.credit_note_no());
            return CbmsSyncResult.ok("Simulated 200 OK: IRD CBMS Credit Note recorded successfully");
        }

        try {
            String response = restClient.post()
                    .uri(cbmsUrl + "/billreturn")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("CBMS Credit Note push succeeded for {}: {}", payload.credit_note_no(), response);
            return CbmsSyncResult.ok(response != null ? response : "200 OK");
        } catch (Exception ex) {
            log.error("CBMS Credit Note push failed for {}: {}", payload.credit_note_no(), ex.getMessage());
            return CbmsSyncResult.fail(500, ex.getMessage());
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
