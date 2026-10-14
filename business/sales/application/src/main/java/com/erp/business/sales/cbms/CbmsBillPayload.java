package com.erp.business.sales.cbms;

import java.math.BigDecimal;

/**
 * Payload compliant with Nepal IRD CBMS Bill API (POST /api/bill).
 */
public record CbmsBillPayload(
        String username,
        String password,
        String seller_pan,
        String buyer_pan,
        String buyer_name,
        String fiscal_year,
        String bill_no,
        String bill_date,
        BigDecimal total_amount,
        BigDecimal discount_amount,
        BigDecimal taxable_amount,
        BigDecimal tax_amount,
        BigDecimal exempt_amount,
        Boolean is_real_time
) {}
