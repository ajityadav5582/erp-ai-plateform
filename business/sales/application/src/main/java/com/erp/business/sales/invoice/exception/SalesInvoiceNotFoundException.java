package com.erp.business.sales.invoice.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a sales invoice id does not resolve within the caller's scope. */
public class SalesInvoiceNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public SalesInvoiceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "SALES_INVOICE_NOT_FOUND", message);
    }

    public SalesInvoiceNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "SALES_INVOICE_NOT_FOUND", message, cause);
    }
}
