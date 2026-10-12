package com.erp.business.sales.customer.domain;

/**
 * Business classification of a customer.
 *
 * <p>The value is a commercial label, not a tax status: whether a sale to this
 * customer is taxable is decided by {@link Customer#isVatRegistered()} together
 * with the invoice's VAT treatment, not by this enum. {@link #EXPORT} is the one
 * exception worth calling out - an export customer is the usual signal that a
 * sale is zero-rated under Nepal IRD rules, so the sales invoice defaults its VAT
 * treatment accordingly.
 */
public enum CustomerType {

    /** Walk-in or occasional buyer, typically not VAT-registered (B2C). */
    RETAIL,

    /** Trade buyer purchasing for resale or production, usually VAT-registered (B2B). */
    WHOLESALE,

    /** Buyer outside Nepal; sales are zero-rated exports. */
    EXPORT,

    /** Government body, ministry or public institution. */
    GOVERNMENT
}
