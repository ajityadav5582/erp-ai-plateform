package com.erpai.integration.ird.outbox.service;

import com.erpai.integration.ird.outbox.entity.IrdInvoiceType;
import com.erpai.integration.ird.port.FiscalBillCommand;
import com.erpai.integration.ird.port.FiscalCreditNoteCommand;

import java.util.UUID;

/**
 * Event published after an IRD sync outbox row has been committed.
 */
public record FiscalBillCommittedEvent(
        UUID outboxId,
        Long tenantId,
        Long companyId,
        Long sourceTransactionId,
        IrdInvoiceType invoiceType,
        FiscalBillCommand billCommand,
        FiscalCreditNoteCommand creditNoteCommand
) {
}
