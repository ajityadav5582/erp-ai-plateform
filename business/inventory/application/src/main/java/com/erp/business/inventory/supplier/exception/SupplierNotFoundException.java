package com.erp.business.inventory.supplier.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

public class SupplierNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public SupplierNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", message);
    }

    public SupplierNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", message, cause);
    }
}
