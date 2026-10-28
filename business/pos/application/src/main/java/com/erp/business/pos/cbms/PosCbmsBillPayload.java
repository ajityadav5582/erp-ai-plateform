package com.erp.business.pos.cbms;

import java.math.BigDecimal;

/**
 * Payload compliant with the Nepal IRD CBMS Bill API (POST /api/bill), shaped
 * for a POS retail sale. Field names match the IRD contract exactly — the
 * snake_case is deliberate and must not be "cleaned up".
 *
 * <p>POS bills are always real-time: IRD requires retail sales to be reported
 * as they happen, so {@code is_real_time} is fixed to {@code true}.
 */
public record PosCbmsBillPayload(
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
) {
}
