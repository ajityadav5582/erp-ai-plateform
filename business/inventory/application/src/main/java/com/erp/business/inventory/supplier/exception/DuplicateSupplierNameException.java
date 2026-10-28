package com.erp.business.inventory.supplier.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class DuplicateSupplierNameException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateSupplierNameException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_SUPPLIER_NAME", message);
    }

    public DuplicateSupplierNameException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "DUPLICATE_SUPPLIER_NAME", message, cause);
    }
}
