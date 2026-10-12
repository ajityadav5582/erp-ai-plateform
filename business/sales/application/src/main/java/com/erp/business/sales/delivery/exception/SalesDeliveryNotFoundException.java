package com.erp.business.sales.delivery.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a sales delivery id does not resolve within the caller's scope. */
public class SalesDeliveryNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public SalesDeliveryNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "SALES_DELIVERY_NOT_FOUND", message);
    }

    public SalesDeliveryNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "SALES_DELIVERY_NOT_FOUND", message, cause);
    }
}
