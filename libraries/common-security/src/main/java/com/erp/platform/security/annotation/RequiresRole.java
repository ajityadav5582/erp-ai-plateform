package com.erp.platform.security.annotation;

import java.lang.annotation.*;

/**
 * Authorization annotation requiring specific roles.
 *
 * <p>When placed on a method, the method will only be accessible
 * to users with the specified roles.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @RequiresRole("ADMIN")
 * public void deleteUser(Long userId) {
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
public @interface RequiresRole {

    /**
     * The required roles.
     *
     * @return the array of roles
     */
    String[] value();
}
