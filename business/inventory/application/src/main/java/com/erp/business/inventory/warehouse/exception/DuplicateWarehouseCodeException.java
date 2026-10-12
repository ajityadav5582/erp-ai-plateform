package com.erp.business.inventory.warehouse.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * A warehouse with this code already exists in the company.
 *
 * <p>Worth distinguishing from the name variant: the code is what stock movements and
 * printed documents reference, so a collision there has to be reported as a code
 * conflict rather than a generic validation failure.
 */
public class DuplicateWarehouseCodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateWarehouseCodeException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_WAREHOUSE_CODE", message);
    }
}
