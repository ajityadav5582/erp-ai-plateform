package com.erp.business.inventory.items.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an item search term is unusable.
 *
 * <p>Reports {@code 400 ITEM_SEARCH_TERM_INVALID}.
 *
 * <p>A blank term is rejected rather than treated as "no filter", because the search
 * endpoint is reached via {@code GET /search} and returning the whole catalogue for an
 * empty query is exactly the unbounded response the paged listings exist to avoid.
 * LIKE metacharacters are not rejected: they are escaped, so a caller searching for a
 * literal {@code %} still gets what they asked for.
 *
 * @since 1.0.0
 */
public class InvalidSearchTermException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public InvalidSearchTermException(String message) {
        super(HttpStatus.BAD_REQUEST, "ITEM_SEARCH_TERM_INVALID", message);
    }
}
