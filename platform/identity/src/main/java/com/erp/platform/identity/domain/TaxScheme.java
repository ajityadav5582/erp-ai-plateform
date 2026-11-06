package com.erp.platform.identity.domain;

/**
 * Tax scheme applicable to a company for Nepal IRD compliance.
 *
 * <p>Determines whether 13% VAT tax invoices (with Annex registers) are issued
 * or whether cash bills under Section 15 with zero VAT line items are used.
 *
 * @since 1.0.0
 */
public enum TaxScheme {

    /**
     * 13% VAT Tax Invoices; Annex registers enabled.
     */
    VAT_REGISTERED,

    /**
     * Cash Bills under Section 15; zero VAT line items.
     */
    PAN_ONLY;

    /**
     * @return {@code true} when VAT is applicable (i.e. {@link #VAT_REGISTERED}).
     */
    public boolean isVatApplicable() {
        return this == VAT_REGISTERED;
    }

    /**
     * @return the default invoice title for this tax scheme.
     */
    public String getDefaultInvoiceTitle() {
        return this == VAT_REGISTERED ? "TAX INVOICE" : "CASH BILL";
    }
}
