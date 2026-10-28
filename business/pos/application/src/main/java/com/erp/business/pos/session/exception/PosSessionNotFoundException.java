package com.erp.business.pos.session.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a session id does not resolve within the caller's scope. */
public class PosSessionNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public PosSessionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "POS_SESSION_NOT_FOUND", message);
    }
}
