package com.erp.business.inventory.categories.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a category name already exists within a company.
 *
 * <p>Reports {@code 409 DUPLICATE_CATEGORY_NAME}.
 *
 * @since 1.0.0
 */
public class DuplicateCategoryNameException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public DuplicateCategoryNameException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_NAME", message);
    }

    public DuplicateCategoryNameException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "DUPLICATE_CATEGORY_NAME", message, cause);
    }
}
