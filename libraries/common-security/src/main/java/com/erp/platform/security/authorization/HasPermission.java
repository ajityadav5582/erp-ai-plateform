package com.erp.platform.security.authorization;

import java.lang.annotation.*;

/**
 * Authorization annotation for permission-based access control.
 *
 * <p>This annotation can be used to enforce permission-based
 * authorization on methods or classes.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @HasPermission("invoice:read:invoice")
 * public Invoice getInvoice(UUID id) {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasPermission {

    /**
     * The permission required to access the annotated element.
     *
     * @return the permission
     */
    String value();

    /**
     * Whether all permissions must be satisfied (AND) or any permission (OR).
     *
     * @return true if all permissions must be satisfied
     */
    boolean requireAll() default true;
}
