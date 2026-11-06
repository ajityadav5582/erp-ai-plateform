package com.erpai.integration.ird.infrastructure.fiscal;

import com.erpai.integration.ird.config.IrdProperties;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.erpai.integration.ird.port.IrdSyncResult;
import com.erpai.integration.ird.port.out.IrdFiscalSyncPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central CBMS HTTP client implementing {@link IrdFiscalSyncPort}.
 *
 * <p>Consolidates the previously duplicated {@code CbmsClient} (sales) and
 * {@code PosCbmsClient} (pos) HTTP logic. Maps the fiscal command to the Nepal
 * IRD CBMS payload shape and authenticates with HTTP Basic auth using the
 * configured IRD credentials. When CBMS is disabled or unconfigured (the default
 * in development), pushes are simulated as successful.
 */
@Slf4j
@Component
public class IrdFiscalSyncAdapter implements IrdFiscalSyncPort {

    private final IrdProperties properties;
    private final RestClient restClient;

    public IrdFiscalSyncAdapter(IrdProperties properties, RestClient irdRestClient) {
        this.properties = properties;
        this.restClient = irdRestClient;
    }

    @Override
    public IrdSyncResult syncSalesBill(FiscalBillCommand command) {
        if (!properties.isEnabled() || properties.getUsername().isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for bill: {}",
                    command.invoiceNumber());
            return new IrdSyncResult(true, 200, "Simulated 200 OK: IRD CBMS Bill recorded successfully", null, true);
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", properties.getUsername());
            payload.put("password", properties.getPassword());
            payload.put("seller_pan", command.sellerPan());
            payload.put("buyer_pan", command.buyerPan());
            payload.put("buyer_name", command.buyerName());
            payload.put("fiscal_year", command.fiscalYear());
            payload.put("bill_no", command.invoiceNumber());
            payload.put("bill_date", command.billDate() != null ? command.billDate().toString() : null);
            payload.put("total_amount", command.totalAmount());
            payload.put("taxable_amount", command.taxableAmount());
            payload.put("tax_amount", command.taxAmount());
            payload.put("exempt_amount", command.exemptAmount());
            payload.put("is_real_time", command.realTime());

            String response = restClient.post()
                    .uri("/bill")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuth())
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("CBMS Bill push succeeded for bill {}: {}", command.invoiceNumber(), response);
            return new IrdSyncResult(true, 200, response != null ? response : "200 OK", null, false);
        } catch (Exception ex) {
            log.error("CBMS Bill push failed for bill {}: {}", command.invoiceNumber(), ex.getMessage());
            return new IrdSyncResult(false, 500, ex.getMessage(), ex.getMessage(), false);
        }
    }

    @Override
    public IrdSyncResult syncCreditNote(FiscalCreditNoteCommand command) {
        if (!properties.isEnabled() || properties.getUsername().isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for return: {}",
                    command.creditNoteNumber());
            return new IrdSyncResult(true, 200, "Simulated 200 OK: IRD CBMS Credit Note recorded successfully", null, true);
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", properties.getUsername());
            payload.put("password", properties.getPassword());
            payload.put("seller_pan", command.customerPan()); // placeholder; seller pan not in command
            payload.put("buyer_pan", command.customerPan());
            payload.put("fiscal_year", command.fiscalYearCode());
            payload.put("ref_bill_no", command.refInvoiceNumber());
            payload.put("credit_note_no", command.creditNoteNumber());
            payload.put("credit_note_date", command.creditNoteDateBs());
            payload.put("reason", command.reason());
            payload.put("total_amount", command.totalAmount());
            payload.put("taxable_amount", command.taxableAmount());
            payload.put("tax_amount", command.vatAmount());
            payload.put("exempt_amount", command.exemptAmount());

            String response = restClient.post()
                    .uri("/billreturn")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuth())
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info("CBMS Credit Note push succeeded for {}: {}", command.creditNoteNumber(), response);
            return new IrdSyncResult(true, 200, response != null ? response : "200 OK", null, false);
        } catch (Exception ex) {
            log.error("CBMS Credit Note push failed for {}: {}", command.creditNoteNumber(), ex.getMessage());
            return new IrdSyncResult(false, 500, ex.getMessage(), ex.getMessage(), false);
        }
    }

    @Override
    public boolean verifyConnection(Long tenantId, Long companyId) {
        return true;
    }

    private String basicAuth() {
        String creds = properties.getUsername() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }
}
