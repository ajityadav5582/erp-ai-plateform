package com.erp.business.pos.cbms;

import java.math.BigDecimal;

public record CbmsBillPayload(
        String seller_pan,
        String buyer_pan,
        String fiscal_year,
        String buyer_name,
        String invoice_number,
        String invoice_date,
        BigDecimal total_sales,
        BigDecimal taxable_sales_vat,
        BigDecimal vat,
        BigDecimal excisable_amount,
        BigDecimal exempt_sales,
        boolean isrealtime,
        String datetime
) {
}
