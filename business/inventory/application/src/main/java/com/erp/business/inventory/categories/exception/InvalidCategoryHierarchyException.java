package com.erp.business.inventory.categories.exception;

import com.erp.platform.common.exception.BaseApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a category operation violates the hierarchy rules.
 *
 * <p>Covers the cases that were previously reported as a bare
 * {@code IllegalArgumentException}, which the generic handler turned into an
 * opaque {@code 500 INTERNAL_ERROR}. These are caller mistakes, not server
 * faults, so they belong on {@code 400 BAD_REQUEST} with a message that names
 * the actual rule that was broken:
 *
 * <ul>
 *   <li>a category cannot be its own parent</li>
 *   <li>a category cannot be moved under one of its own descendants (cycle)</li>
 *   <li>the parent category must belong to the same company</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class InvalidCategoryHierarchyException extends BaseApiException {

    private static final long serialVersionUID = 1L;

    public InvalidCategoryHierarchyException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_HIERARCHY", message);
    }

    public InvalidCategoryHierarchyException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_HIERARCHY", message, cause);
    }
}
