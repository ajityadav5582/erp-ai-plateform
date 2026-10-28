package com.erp.business.inventory.categories.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a category is not found.
 *
 * <p>Reports {@code 404 CATEGORY_NOT_FOUND}.
 *
 * @since 1.0.0
 */
public class CategoryNotFoundException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public CategoryNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", message);
    }

    public CategoryNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", message, cause);
    }
}
