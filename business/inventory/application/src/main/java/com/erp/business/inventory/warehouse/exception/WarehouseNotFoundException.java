package com.erp.business.inventory.warehouse.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** The requested warehouse does not exist in the caller's company, or does not exist. */
public class WarehouseNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public WarehouseNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND", message);
    }

    public WarehouseNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND", message, cause);
    }
}
