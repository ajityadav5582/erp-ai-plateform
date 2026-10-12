package com.erp.business.sales.customer.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a customer code already exists within the same company. */
public class DuplicateCustomerCodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateCustomerCodeException(String message) {
        super(HttpStatus.CONFLICT, "CUSTOMER_CODE_DUPLICATE", message);
    }

    public DuplicateCustomerCodeException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "CUSTOMER_CODE_DUPLICATE", message, cause);
    }
}
