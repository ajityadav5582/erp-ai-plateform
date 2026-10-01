package com.erp.business.inventory.domain;

/** Taxability classification under Nepal VAT Act 2052. */
public enum TaxabilityType {
    TAXABLE,     // Subject to standard VAT (13%)
    EXEMPT,      // VAT Exempt under Schedule 1 of VAT Act 2052 (कर छूट)
    ZERO_RATED   // 0% VAT for Exports under Section 10 of VAT Act 2052 (शून्य दर)
}
