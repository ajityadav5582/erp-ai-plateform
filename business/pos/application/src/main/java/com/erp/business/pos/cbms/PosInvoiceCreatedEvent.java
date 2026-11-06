package com.erp.business.pos.cbms;

import org.springframework.context.ApplicationEvent;

public class PosInvoiceCreatedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;
    private final Long transactionId;
    public PosInvoiceCreatedEvent(Object source, Long transactionId) {
        super(source);
        this.transactionId = transactionId;
    }
    public Long getTransactionId() { return transactionId; }
}
