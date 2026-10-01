package com.erp.business.inventory.categories.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a category slug already exists within a company.
 *
 * <p>Reports {@code 409 DUPLICATE_CATEGORY_SLUG}.
 *
 * @since 1.0.0
 */
public class DuplicateCategorySlugException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateCategorySlugException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_SLUG", message);
    }

    public DuplicateCategorySlugException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_SLUG", message, cause);
    }
}
