package com.erp.business.pos.register.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a register id does not resolve within the caller's scope. */
public class PosRegisterNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public PosRegisterNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "POS_REGISTER_NOT_FOUND", message);
    }
}
