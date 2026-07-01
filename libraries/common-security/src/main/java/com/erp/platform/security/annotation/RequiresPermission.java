package com.erp.platform.security.annotation;

import java.lang.annotation.*;

/**
 * Authorization annotation requiring specific permissions.
 *
 * <p>When placed on a method, the method will only be accessible
 * to users with the specified permissions.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @RequiresPermission("invoice:read")
 * public Invoice getInvoice(Long id) {
 *     ...
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * The required permissions.
     *
     * @return the array of permissions
     */
    String[] value();

    /**
     * The logical operator for multiple permissions.
     *
     * @return the logical operator
     */
    LogicalOperator logical() default LogicalOperator.AND;

    /**
     * Logical operators for permission checking.
     */
    enum LogicalOperator {
        /** All permissions must be granted */
        AND,
        /** Any permission must be granted */
        OR
    }
}
