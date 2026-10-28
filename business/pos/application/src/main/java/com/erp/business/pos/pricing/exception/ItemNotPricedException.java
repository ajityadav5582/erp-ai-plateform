package com.erp.business.pos.pricing.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/** Thrown when an item cannot be priced (missing from the catalogue or no selling price). */
public class ItemNotPricedException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public ItemNotPricedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "ITEM_NOT_PRICED", message);
    }
}
