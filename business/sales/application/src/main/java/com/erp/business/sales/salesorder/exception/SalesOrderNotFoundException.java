package com.erp.business.sales.salesorder.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a sales order id does not resolve within the caller's scope. */
public class SalesOrderNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public SalesOrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "SALES_ORDER_NOT_FOUND", message);
    }

    public SalesOrderNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "SALES_ORDER_NOT_FOUND", message, cause);
    }
}
