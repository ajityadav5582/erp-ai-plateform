package com.erp.business.inventory.unit.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class DuplicateUnitNameException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateUnitNameException(String message) {
        super(HttpStatus.CONFLICT, "UNIT_NAME_DUPLICATE", message);
    }

    public DuplicateUnitNameException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "UNIT_NAME_DUPLICATE", message, cause);
    }
}
