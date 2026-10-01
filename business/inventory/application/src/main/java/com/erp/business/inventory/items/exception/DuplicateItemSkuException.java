package com.erp.business.inventory.items.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an item SKU is already taken within the company.
 *
 * <p>Reports {@code 409 ITEM_SKU_DUPLICATE}.
 *
 * <p>A conflict rather than a bad request: the request is well-formed and the SKU is
 * legal, it simply collides with a record that already exists. The caller can resolve
 * it by choosing a different SKU without changing anything else.
 *
 * <p>This previously extended {@link RuntimeException} and so surfaced as
 * {@code 500 INTERNAL_ERROR}, telling the user the service had crashed when in fact
 * their SKU was taken.
 *
 * @since 1.0.0
 */
public class DuplicateItemSkuException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateItemSkuException(String sku) {
        super(HttpStatus.CONFLICT, "ITEM_SKU_DUPLICATE",
                "An item with SKU '" + sku + "' already exists for this company");
    }

    public DuplicateItemSkuException(String sku, Throwable cause) {
        super(HttpStatus.CONFLICT, "ITEM_SKU_DUPLICATE",
                "An item with SKU '" + sku + "' already exists for this company", cause);
    }
}
