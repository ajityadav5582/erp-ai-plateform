package com.erp.business.inventory.warehouse.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * The requested change is legal in principle but refused by a business rule - an
 * inactive warehouse cannot become the default, and the current default cannot be
 * deactivated.
 *
 * <p>Distinct from {@link WarehouseNotFoundException} because the warehouse does exist
 * and the caller addressed it correctly. Reporting a conflict keeps the distinction
 * between "you asked for something that is not there" and "that exists, but not like
 * that" visible to the client.
 */
public class WarehouseOperationException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public WarehouseOperationException(String message) {
        super(HttpStatus.CONFLICT, "WAREHOUSE_OPERATION_NOT_ALLOWED", message);
    }

    public WarehouseOperationException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "WAREHOUSE_OPERATION_NOT_ALLOWED", message, cause);
    }
}
