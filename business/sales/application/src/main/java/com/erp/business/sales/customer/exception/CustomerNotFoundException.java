package com.erp.business.sales.customer.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a customer id does not resolve within the caller's scope. */
public class CustomerNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public CustomerNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", message);
    }

    public CustomerNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", message, cause);
    }
}
