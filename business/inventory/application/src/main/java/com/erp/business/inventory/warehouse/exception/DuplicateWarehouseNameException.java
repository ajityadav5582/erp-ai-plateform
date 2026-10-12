package com.erp.business.inventory.warehouse.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** A warehouse with this name already exists in the company. */
public class DuplicateWarehouseNameException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateWarehouseNameException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_WAREHOUSE_NAME", message);
    }
}
