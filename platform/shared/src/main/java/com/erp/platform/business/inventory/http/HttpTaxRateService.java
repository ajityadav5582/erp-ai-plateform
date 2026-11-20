package com.erp.platform.business.inventory.http;

import com.erp.business.inventory.interfaces.tax.TaxRateService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class HttpTaxRateService implements TaxRateService {

    private static final String TAX_RATES_PATH = "/api/v1/inventory/tax-rates";

    private final RestClient restClient;

    public HttpTaxRateService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public TaxRate findActive(Long tenantId, Long companyId, String code, LocalDate asOf) {
        try {
            List<TaxRateDto> list = restClient.get()
                    .uri(TAX_RATES_PATH)
                    .headers(headers -> {
                        forwardRequestContext(headers);
                        if (tenantId != null) headers.set("X-Tenant-Id", String.valueOf(tenantId));
                        if (companyId != null) headers.set("X-Company-Id", String.valueOf(companyId));
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TaxRateDto>>() {});

            if (list == null || list.isEmpty()) {
                return defaultVatRate(code);
            }

            LocalDate date = asOf != null ? asOf : LocalDate.now();
            return list.stream()
                    .filter(tr -> code == null || code.equalsIgnoreCase(tr.code()))
                    .filter(tr -> tr.effectiveFrom() == null || !tr.effectiveFrom().isAfter(date))
                    .filter(tr -> tr.effectiveTo() == null || !tr.effectiveTo().isBefore(date))
                    .findFirst()
                    .map(tr -> new TaxRate(tr.id(), tr.code(), tr.name(), tr.rate(), tr.applicability(), tr.effectiveFrom(), tr.effectiveTo()))
                    .orElseGet(() -> defaultVatRate(code));
        } catch (Exception e) {
            return defaultVatRate(code);
        }
    }

    private TaxRate defaultVatRate(String code) {
        if ("VAT".equalsIgnoreCase(code)) {
            return new TaxRate(1L, "VAT", "Value Added Tax (13%)", new BigDecimal("13.00"), "STANDARD", LocalDate.of(2000, 1, 1), null);
        }
        return new TaxRate(0L, code, code, BigDecimal.ZERO, "ZERO", LocalDate.of(2000, 1, 1), null);
    }

    private void forwardRequestContext(HttpHeaders headers) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        copyHeader(request, headers, "Authorization");
        copyHeader(request, headers, "X-Company-Id");
        copyHeader(request, headers, "X-Fiscal-Year-Id");
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TaxRateDto(Long id, String code, String name, BigDecimal rate,
                      String applicability, LocalDate effectiveFrom, LocalDate effectiveTo) {}
}

