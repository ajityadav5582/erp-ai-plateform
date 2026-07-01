package com.erp.platform.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation to inject the current authenticated user.
 *
 * <p>Can be used on method parameters to automatically inject
 * the current authenticated user from the security context.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * public void updateProfile(@CurrentUser AuthenticatedUser user, ProfileRequest request) {
 *     ...
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
