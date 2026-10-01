package com.erp.business.inventory.unit.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class DuplicateUnitCodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateUnitCodeException(String message) {
        super(HttpStatus.CONFLICT, "UNIT_CODE_DUPLICATE", message);
    }

    public DuplicateUnitCodeException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "UNIT_CODE_DUPLICATE", message, cause);
    }
}
