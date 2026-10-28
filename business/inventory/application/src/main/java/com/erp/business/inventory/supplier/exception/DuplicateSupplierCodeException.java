package com.erp.business.inventory.supplier.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class DuplicateSupplierCodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateSupplierCodeException(String message) {
        super(HttpStatus.CONFLICT, "SUPPLIER_CODE_DUPLICATE", message);
    }

    public DuplicateSupplierCodeException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "SUPPLIER_CODE_DUPLICATE", message, cause);
    }
}
