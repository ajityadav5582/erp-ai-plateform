package com.erpai.integration.ird.infrastructure.fiscal;

import com.erpai.integration.ird.config.IrdProperties;
import com.erpai.integration.ird.domain.CompanyIrdConfig;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.erpai.integration.ird.port.IrdSyncResult;
import com.erpai.integration.ird.port.out.IrdCryptoPort;
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
 * configured IRD credentials. Supports multi-company dynamic credentials resolution
 * from {@link CompanyIrdConfig}. When CBMS is disabled or unconfigured,
 * pushes are simulated as successful.
 */
@Slf4j
@Component
public class IrdFiscalSyncAdapter implements IrdFiscalSyncPort {

    private final IrdProperties properties;
    private final RestClient restClient;
    private final IrdCryptoPort cryptoPort;

    public IrdFiscalSyncAdapter(IrdProperties properties, RestClient irdRestClient, IrdCryptoPort cryptoPort) {
        this.properties = properties;
        this.restClient = irdRestClient;
        this.cryptoPort = cryptoPort;
    }

    @Override
    public IrdSyncResult syncSalesBill(FiscalBillCommand command) {
        return syncSalesBill(command, null);
    }

    @Override
    public IrdSyncResult syncSalesBill(FiscalBillCommand command, CompanyIrdConfig companyConfig) {
        String username = resolveUsername(companyConfig);
        String password = resolvePassword(companyConfig);
        boolean isEnabled = isSyncEnabled(companyConfig);

        if (!isEnabled || username == null || username.isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for bill: {}",
                    command.invoiceNumber());
            return new IrdSyncResult(true, 200, "Simulated 200 OK: IRD CBMS Bill recorded successfully", null, true);
        }

        try {
            String sellerPan = (companyConfig != null && companyConfig.getSellerPan() != null)
                    ? companyConfig.getSellerPan().value()
                    : command.sellerPan();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            payload.put("seller_pan", sellerPan);
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
                    .header("Authorization", basicAuth(username, password))
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
        return syncCreditNote(command, null);
    }

    @Override
    public IrdSyncResult syncCreditNote(FiscalCreditNoteCommand command, CompanyIrdConfig companyConfig) {
        String username = resolveUsername(companyConfig);
        String password = resolvePassword(companyConfig);
        boolean isEnabled = isSyncEnabled(companyConfig);

        if (!isEnabled || username == null || username.isBlank()) {
            log.info("CBMS live sync disabled or unconfigured. Simulating successful IRD push for return: {}",
                    command.creditNoteNumber());
            return new IrdSyncResult(true, 200, "Simulated 200 OK: IRD CBMS Credit Note recorded successfully", null, true);
        }

        try {
            String sellerPan = (companyConfig != null && companyConfig.getSellerPan() != null)
                    ? companyConfig.getSellerPan().value()
                    : command.customerPan();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", username);
            payload.put("password", password);
            payload.put("seller_pan", sellerPan);
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
                    .header("Authorization", basicAuth(username, password))
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

    private String resolveUsername(CompanyIrdConfig companyConfig) {
        if (companyConfig != null && companyConfig.getApiUsername() != null && !companyConfig.getApiUsername().isBlank()) {
            return companyConfig.getApiUsername();
        }
        return properties.getUsername();
    }

    private String resolvePassword(CompanyIrdConfig companyConfig) {
        if (companyConfig != null && companyConfig.getApiPasswordEncrypted() != null && !companyConfig.getApiPasswordEncrypted().isBlank()) {
            try {
                return cryptoPort != null ? cryptoPort.decrypt(companyConfig.getApiPasswordEncrypted()) : companyConfig.getApiPasswordEncrypted();
            } catch (Exception e) {
                log.warn("Failed to decrypt company CBMS password, falling back to static config: {}", e.getMessage());
            }
        }
        return properties.getPassword();
    }

    private boolean isSyncEnabled(CompanyIrdConfig companyConfig) {
        if (companyConfig != null) {
            return companyConfig.isCbmsActive();
        }
        return properties.isEnabled();
    }

    private String basicAuth(String username, String password) {
        String creds = (username != null ? username : "") + ":" + (password != null ? password : "");
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }
}
