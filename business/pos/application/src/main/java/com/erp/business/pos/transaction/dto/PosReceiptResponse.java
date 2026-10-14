package com.erp.business.pos.transaction.dto;

/**
 * Receipt payload: everything a printed or on-screen receipt needs, in one call.
 *
 * <p>The transaction carries its own frozen snapshots (cashier, warehouse,
 * customer, lines, totals); the receipt adds the session and register context
 * that lives on the owning aggregates, so the terminal does not need three
 * extra round-trips to render a slip.
 *
 * @param transaction  the full transaction read model
 * @param sessionNumber the shift the sale was rung on (e.g. SES-000042)
 * @param registerCode register the sale was rung on (e.g. REG-KTM-01)
 * @param registerName register display name for the receipt header
 */
public record PosReceiptResponse(

        PosTransactionResponse transaction,

        String sessionNumber,

        String registerCode,

        String registerName
) {
}
