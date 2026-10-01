package com.erp.business.inventory.unit.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class UnitNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public UnitNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "UNIT_NOT_FOUND", message);
    }

    public UnitNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "UNIT_NOT_FOUND", message, cause);
    }
}
