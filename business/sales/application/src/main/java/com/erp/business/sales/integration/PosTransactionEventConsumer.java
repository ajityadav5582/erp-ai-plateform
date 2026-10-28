package com.erp.business.sales.integration;

import com.erp.business.sales.invoice.SalesInvoiceService;
import com.erp.business.sales.invoice.dto.CreatePosInvoiceRequest;
import com.erp.business.sales.invoice.dto.InvoiceLineRequest;
import com.erp.business.sales.invoice.domain.VatType;
import com.erp.platform.common.context.RequestContext;
import com.erp.platform.common.tenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class PosTransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PosTransactionEventConsumer.class);
    public static final String TOPIC = "pos.transaction.completed";

    private final ObjectMapper objectMapper;
    private final SalesInvoiceService salesInvoiceService;
    private final String serviceToken;

    public PosTransactionEventConsumer(ObjectMapper objectMapper,
                                      SalesInvoiceService salesInvoiceService,
                                      @Value("${erp.integration.service-token}") String serviceToken) {
        this.objectMapper = objectMapper;
        this.salesInvoiceService = salesInvoiceService;
        this.serviceToken = serviceToken;
    }

    @KafkaListener(topics = TOPIC, groupId = "sales-service")
    public void handle(ConsumerRecord<String, String> record) {
        // Service-to-service trust check.
        String token = null;
        if (record.headers().lastHeader("X-Service-Token") != null) {
            token = new String(record.headers().lastHeader("X-Service-Token").value(), StandardCharsets.UTF_8);
        }
        if (token == null || !token.equals(serviceToken)) {
            throw new SecurityException("Invalid service token in POS event");
        }

        try {
            PosTransactionCompletedEvent event = objectMapper.readValue(record.value(), PosTransactionCompletedEvent.class);

            TenantContext.setTenantId(event.tenantId());
            RequestContext.setTenantId(event.tenantId());
            RequestContext.setCompanyId(String.valueOf(event.companyId()));
            if (event.fiscalYearId() != null) {
                RequestContext.setFiscalYearId(String.valueOf(event.fiscalYearId()));
            }
            if (event.cashierUserId() != null) {
                RequestContext.setUserId(event.cashierUserId());
            }

            List<InvoiceLineRequest> lines = event.lines().stream()
                    .map(l -> new InvoiceLineRequest(
                            l.itemId(),
                            l.uomCode(),
                            l.quantity(),
                            l.unitPrice(),
                            l.discountPercentage(),
                            VatType.valueOf(l.vatType()),
                            l.vatRate(),
                            l.description()))
                    .toList();

            CreatePosInvoiceRequest request = new CreatePosInvoiceRequest(
                    event.posTransactionId(),
                    event.customerId(),
                    event.customerName(),
                    event.customerPanNumber(),
                    event.invoiceDate(),
                    event.notes(),
                    lines);

            salesInvoiceService.createPosInvoice(Long.parseLong(event.tenantId()), event.companyId(), request);
        } catch (Exception ex) {
            log.warn("Failed to process POS transaction event: {}", ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            TenantContext.clear();
            RequestContext.clear();
        }
    }
}
