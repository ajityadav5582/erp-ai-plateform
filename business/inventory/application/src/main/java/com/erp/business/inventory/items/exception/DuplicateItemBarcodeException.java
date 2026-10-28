package com.erp.business.inventory.items.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an item barcode is already taken within the company.
 *
 * <p>Reports {@code 409 ITEM_BARCODE_DUPLICATE}.
 *
 * <p>A conflict rather than a bad request: the request is well-formed and the barcode
 * is legal, it simply collides with a record that already exists.
 *
 * <p>This previously extended {@link RuntimeException} and so surfaced as
 * {@code 500 INTERNAL_ERROR}. Because a barcode is what a shop scanner sends, a
 * mis-typed barcode was reported to staff as a server fault.
 *
 * @since 1.0.0
 */
public class DuplicateItemBarcodeException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateItemBarcodeException(String barcode) {
        super(HttpStatus.CONFLICT, "ITEM_BARCODE_DUPLICATE",
                "An item with barcode '" + barcode + "' already exists for this company");
    }

    public DuplicateItemBarcodeException(String barcode, Throwable cause) {
        super(HttpStatus.CONFLICT, "ITEM_BARCODE_DUPLICATE",
                "An item with barcode '" + barcode + "' already exists for this company", cause);
    }
}
