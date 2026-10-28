package com.erp.business.sales.invoice.domain;

import java.math.BigDecimal;

/**
 * Nepal IRD VAT treatment of a sale.
 *
 * <p>The treatment decides whether VAT is charged and at what rate, and it is what
 * the sales register and the VAT return (VAT-201) are broken down by:
 * <ul>
 *   <li>{@link #TAXABLE} - standard-rated, VAT at {@link #STANDARD_RATE} (13%).</li>
 *   <li>{@link #EXEMPT} - statutorily exempt; no VAT, but recorded separately.</li>
 *   <li>{@link #ZERO_RATED} - 0% with input-VAT credit (e.g. certain local supplies).</li>
 *   <li>{@link #EXPORT} - 0% on exports; recorded separately from zero-rated.</li>
 * </ul>
 */
public enum VatType {

    /** Standard-rated supply. Nepal's standard VAT rate. */
    TAXABLE,

    /** Statutorily exempt supply. No VAT charged. */
    EXEMPT,

    /** Zero-rated supply (0%, input VAT creditable). */
    ZERO_RATED,

    /** Export of goods or services (0%). */
    EXPORT;

    /** Nepal's standard VAT rate, 13%. */
    public static final BigDecimal STANDARD_RATE = new BigDecimal("13.00");
}
