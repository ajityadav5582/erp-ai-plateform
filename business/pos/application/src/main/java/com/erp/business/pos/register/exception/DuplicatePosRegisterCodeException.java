package com.erp.business.pos.register.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a register code is already used within the caller's company. */
public class DuplicatePosRegisterCodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicatePosRegisterCodeException(String message) {
        super(HttpStatus.CONFLICT, "POS_REGISTER_CODE_EXISTS", message);
    }
}
