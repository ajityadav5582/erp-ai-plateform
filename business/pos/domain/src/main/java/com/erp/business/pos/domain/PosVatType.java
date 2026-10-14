package com.erp.business.pos.domain;

/**
 * VAT treatment applied to a POS transaction line.
 *
 * <p>Mirrors the VAT classification used in sales invoices for consistency
 * with Nepal IRD CBMS reporting requirements.
 */
public enum PosVatType {

    /** Subject to the standard VAT rate (13% in Nepal). */
    TAXABLE,

    /** Exempt from VAT — no VAT charged or recoverable. */
    EXEMPT,

    /** Zero-rated — VAT rate is 0%, distinct from exempt for IRD classification. */
    ZERO_RATED,

    /** Export — goods leaving Nepal, zero-rated for export. */
    EXPORT
}
