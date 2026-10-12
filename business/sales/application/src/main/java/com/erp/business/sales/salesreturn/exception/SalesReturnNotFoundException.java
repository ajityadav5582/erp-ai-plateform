package com.erp.business.sales.salesreturn.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a sales return id does not resolve within the caller's scope. */
public class SalesReturnNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public SalesReturnNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "SALES_RETURN_NOT_FOUND", message);
    }

    public SalesReturnNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "SALES_RETURN_NOT_FOUND", message, cause);
    }
}
