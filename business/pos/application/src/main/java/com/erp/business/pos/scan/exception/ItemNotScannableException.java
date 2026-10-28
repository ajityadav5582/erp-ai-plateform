package com.erp.business.pos.scan.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a scanned code does not resolve to any item in the caller's scope. */
public class ItemNotScannableException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public ItemNotScannableException(String message) {
        super(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", message);
    }
}
