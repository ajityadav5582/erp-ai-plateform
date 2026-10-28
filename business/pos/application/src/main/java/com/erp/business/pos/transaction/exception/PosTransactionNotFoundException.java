package com.erp.business.pos.transaction.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a transaction id or number does not resolve within the caller's scope. */
public class PosTransactionNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public PosTransactionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "POS_TRANSACTION_NOT_FOUND", message);
    }
}
