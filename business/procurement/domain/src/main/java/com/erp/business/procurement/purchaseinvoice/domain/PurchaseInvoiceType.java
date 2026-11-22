package com.erp.business.procurement.purchaseinvoice.domain;

/**
 * Classification of a purchase invoice by its procurement origin.
 */
public enum PurchaseInvoiceType {

    /**
     * Standard 3-way match: Billed against a physical warehouse delivery (Goods Receipt Note).
     */
    GOODS_RECEIPT,

    /**
     * Direct supplier bill: Services, SaaS subscriptions, utilities, consulting, maintenance,
     * or other direct non-inventory expenses with no warehouse receipt note.
     */
    DIRECT
}

