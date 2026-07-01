package com.erp.platform.security.authorization;

import java.lang.annotation.*;

/**
 * Authorization annotation for role-based access control.
 *
 * <p>This annotation can be used to enforce role-based
 * authorization on methods or classes.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @HasRole("ADMIN")
 * public void deleteInvoice(UUID id) {
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
public @interface HasRole {

    /**
     * The role(s) required to access the annotated element.
     *
     * @return the roles
     */
    String[] value();

    /**
     * Whether all roles must be satisfied (AND) or any role (OR).
     *
     * @return true if all roles must be satisfied
     */
    boolean requireAll() default false;
}
