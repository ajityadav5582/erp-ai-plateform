package com.erp.business.sales.receipt.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a customer receipt id does not resolve within the caller's scope. */
public class CustomerReceiptNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public CustomerReceiptNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_RECEIPT_NOT_FOUND", message);
    }

    public CustomerReceiptNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_RECEIPT_NOT_FOUND", message, cause);
    }
}
