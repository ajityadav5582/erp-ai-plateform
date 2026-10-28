package com.erp.business.sales.cbms;

import java.math.BigDecimal;

/**
 * Payload compliant with Nepal IRD CBMS Bill Return API (POST /api/billreturn).
 */
public record CbmsBillReturnPayload(
        String username,
        String password,
        String seller_pan,
        String buyer_pan,
        String fiscal_year,
        String ref_bill_no,
        String credit_note_no,
        String credit_note_date,
        String reason,
        BigDecimal total_amount,
        BigDecimal taxable_amount,
        BigDecimal tax_amount
) {}
