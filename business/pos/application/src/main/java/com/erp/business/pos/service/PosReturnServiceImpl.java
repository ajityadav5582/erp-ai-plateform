package com.erp.business.pos.service;

import com.erp.business.pos.domain.PosTransaction;
import com.erp.business.pos.domain.PosTransactionLine;
import com.erp.business.pos.domain.PosTransactionStatus;
import com.erp.business.pos.transaction.infrastructure.persistence.PosTransactionRepository;
import com.erpai.integration.ird.outbox.service.IrdOutboxService;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PosReturnServiceImpl implements PosReturnService {

    private final PosTransactionRepository transactionRepository;
    private final IrdOutboxService irdOutboxService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.erp.business.pos.service.sequence.FiscalSequenceService fiscalSequenceService;

    public PosReturnServiceImpl(PosTransactionRepository transactionRepository,
                               IrdOutboxService irdOutboxService,
                               ApplicationEventPublisher eventPublisher,
                               com.erp.business.pos.service.sequence.FiscalSequenceService fiscalSequenceService) {
        this.transactionRepository = transactionRepository;
        this.irdOutboxService = irdOutboxService;
        this.eventPublisher = eventPublisher;
        this.fiscalSequenceService = fiscalSequenceService;
    }

    @Override
    public void processSalesReturn(Long originalTransactionId, ReturnRequestDto dto) {
        PosTransaction original = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Original transaction not found: " + originalTransactionId));

        if (original.getStatus() == PosTransactionStatus.VOIDED) {
            throw new IllegalStateException("Original transaction is already voided");
        }

        List<PosTransactionLine> creditLines = new ArrayList<>();
        int lineNo = 1;
        for (PosTransactionLine originalLine : original.getLines()) {
            PosTransactionLine creditLine = PosTransactionLine.builder()
                    .lineNumber(lineNo++)
                    .itemId(originalLine.getItemId())
                    .itemCode(originalLine.getItemCode())
                    .itemName(originalLine.getItemName())
                    .barcode(originalLine.getBarcode())
                    .uomCode(originalLine.getUomCode())
                    .quantity(originalLine.getQuantity().negate())
                    .unitPrice(originalLine.getUnitPrice())
                    .build();
            creditLines.add(creditLine);
        }

        PosTransaction creditNote = PosTransaction.builder()
                .transactionNumber(fiscalSequenceService.nextNumber(
                        original.getTenantId(), original.getCompanyId(), null, "CREDIT_NOTE", "CN-83/84-"))
                .customerName(original.getCustomerName())
                .customerPanNumber(original.getCustomerPanNumber())
                .currency(original.getCurrency())
                .status(PosTransactionStatus.CREDIT_NOTE)
                .totalAmount(original.getTotalAmount().abs())
                .taxableAmount(original.getTaxableAmount().abs())
                .vatAmount(original.getVatAmount().abs())
                .lines(creditLines)
                .build();

        creditNote.assignScope(original.getTenantId(), original.getCompanyId());
        creditNote = transactionRepository.save(creditNote);

        original.voidTransaction(null, dto.reason());
        transactionRepository.save(original);

        FiscalCreditNoteCommand command = new FiscalCreditNoteCommand(
                original.getTenantId(),
                original.getCompanyId(),
                creditNote.getTransactionNumber(),
                null,
                original.getTransactionNumber(),
                null,
                original.getCustomerPanNumber(),
                original.getCustomerName(),
                creditNote.getTotalAmount(),
                creditNote.getTaxableAmount(),
                creditNote.getVatAmount(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                dto.reason(),
                true
        );

        irdOutboxService.enqueueCreditNote(
                original.getTenantId(),
                original.getCompanyId(),
                creditNote.getId(),
                command
        );

    }

    private String nextCreditNoteNumber() {
        return "CN-83/84-0000001";
    }
}
