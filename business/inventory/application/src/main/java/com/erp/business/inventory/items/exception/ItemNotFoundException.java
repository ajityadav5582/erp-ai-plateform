package com.erp.business.inventory.items.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an item cannot be found.
 *
 * <p>Reports {@code 404 ITEM_NOT_FOUND}.
 *
 * <p>This previously extended {@link RuntimeException}, which meant it fell through
 * every handler in {@code AbstractGlobalExceptionHandler} to the catch-all and
 * surfaced as {@code 500 INTERNAL_ERROR}. A client asking for an id that does not
 * exist was told the service was broken, which is both wrong and unactionable.
 *
 * <p>Both constructors are retained because the service raises this two ways: from an
 * id lookup ({@link #ItemNotFoundException(Long)}) and from a natural-key lookup by
 * SKU or barcode ({@link #ItemNotFoundException(String)}).
 *
 * @since 1.0.0
 */
public class ItemNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", message);
    }

    public ItemNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", message, cause);
    }

    public ItemNotFoundException(Long itemId) {
        super(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "Item not found with id: " + itemId);
    }
}
