package com.erp.business.sales.customer.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a customer name already exists within the same company. */
public class DuplicateCustomerNameException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateCustomerNameException(String message) {
        super(HttpStatus.CONFLICT, "CUSTOMER_NAME_DUPLICATE", message);
    }

    public DuplicateCustomerNameException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "CUSTOMER_NAME_DUPLICATE", message, cause);
    }
}
