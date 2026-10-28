package com.erp.business.pos.quickitem.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a quick item id does not resolve within the caller's scope. */
public class PosQuickItemNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public PosQuickItemNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "POS_QUICK_ITEM_NOT_FOUND", message);
    }
}
