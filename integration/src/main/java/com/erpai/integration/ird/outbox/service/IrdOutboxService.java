package com.erpai.integration.ird.outbox.service;

import com.erpai.integration.ird.outbox.entity.IrdInvoiceType;
import com.erpai.integration.ird.outbox.entity.IrdSourceModule;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxEntity;
import com.erpai.integration.ird.outbox.entity.IrdSyncOutboxStatus;
import com.erpai.integration.ird.outbox.repository.IrdSyncOutboxRepository;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class IrdOutboxService {

    private final ObjectMapper objectMapper;
    private final IrdSyncOutboxRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public IrdOutboxService(ObjectMapper objectMapper,
                            IrdSyncOutboxRepository repository,
                            ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UUID enqueueSalesBill(Long tenantId, Long companyId, Long sourceTransactionId, FiscalBillCommand command) {
        return enqueueSalesBill(tenantId, companyId, sourceTransactionId, command, IrdSourceModule.POS);
    }

    @Transactional
    public UUID enqueueSalesBill(Long tenantId, Long companyId, Long sourceTransactionId, FiscalBillCommand command,
                                com.erpai.integration.ird.outbox.entity.IrdSourceModule sourceModule) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FiscalBillCommand", e);
        }

        IrdSyncOutboxEntity entity = IrdSyncOutboxEntity.builder()
                .tenantId(tenantId)
                .companyId(companyId)
                .sourceModule(sourceModule)
                .sourceTransactionId(sourceTransactionId)
                .invoiceNumber(command.invoiceNumber())
                .invoiceType(IrdInvoiceType.SALES_BILL)
                .payloadJson(payloadJson)
                .syncStatus(IrdSyncOutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(5)
                .nextRetryAt(Instant.now())
                .build();

        entity = repository.save(entity);

        eventPublisher.publishEvent(new FiscalBillCommittedEvent(
                entity.getId(), tenantId, companyId, sourceTransactionId,
                IrdInvoiceType.SALES_BILL, command, null));

        return entity.getId();
    }

    @Transactional
    public UUID enqueueCreditNote(Long tenantId, Long companyId, Long sourceTransactionId, FiscalCreditNoteCommand command) {
        return enqueueCreditNote(tenantId, companyId, sourceTransactionId, command, IrdSourceModule.POS);
    }

    @Transactional
    public UUID enqueueCreditNote(Long tenantId, Long companyId, Long sourceTransactionId, FiscalCreditNoteCommand command,
                                 com.erpai.integration.ird.outbox.entity.IrdSourceModule sourceModule) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize FiscalCreditNoteCommand", e);
        }

        IrdSyncOutboxEntity entity = IrdSyncOutboxEntity.builder()
                .tenantId(tenantId)
                .companyId(companyId)
                .sourceModule(sourceModule)
                .sourceTransactionId(sourceTransactionId)
                .invoiceNumber(command.creditNoteNumber())
                .invoiceType(IrdInvoiceType.CREDIT_NOTE)
                .payloadJson(payloadJson)
                .syncStatus(IrdSyncOutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(5)
                .nextRetryAt(Instant.now())
                .build();

        entity = repository.save(entity);

        eventPublisher.publishEvent(new FiscalBillCommittedEvent(
                entity.getId(), tenantId, companyId, sourceTransactionId,
                IrdInvoiceType.CREDIT_NOTE, null, command));

        return entity.getId();
    }
}
