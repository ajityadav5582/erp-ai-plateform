package com.erp.platform.identity.domain.exception;

/**
 * Exception thrown when a circular parent-child relationship is detected in department hierarchy.
 *
 * @since 1.0.0
 */
public class CircularDepartmentHierarchyException extends DepartmentOperationException {

    private static final long serialVersionUID = 1L;

    public CircularDepartmentHierarchyException(String message) {
        super(message);
    }
}
