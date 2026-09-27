package com.erp.platform.identity.application.security;

import java.lang.annotation.*;

/**
 * Annotation to enforce permission-based access control on controller methods.
 *
 * <p>When applied to a Spring MVC controller method, the {@link PermissionAspect}
 * intercepts the call and verifies that the authenticated user has the required
 * permission(s) in their tenant.
 *
 * <p>SUPER_ADMIN users bypass all permission checks.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @RequirePermission("USER_READ")
 * @GetMapping("/{userId}")
 * public UserResponse getUser(@PathVariable Long userId) {
 *     ...
 * }
 *
 * @RequirePermission(value = {"USER_CREATE", "USER_UPDATE"}, requireAll = false)
 * @PostMapping
 * public UserResponse createUser(@RequestBody CreateUserRequest request) {
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
public @interface RequirePermission {

    /**
     * The permission codes required to access the annotated method.
     *
     * <p>Permission codes follow the format: {@code RESOURCE_ACTION}
     * (e.g., "USER_READ", "ROLE_CREATE", "PERMISSION_DELETE").
     *
     * @return the required permission codes
     */
    String[] value();

    /**
     * If true, the user must have ALL specified permissions.
     * If false, the user needs ANY ONE of the specified permissions.
     *
     * <p>Default is false (any one permission is sufficient).
     *
     * @return true if all permissions are required, false if any one is sufficient
     */
    boolean requireAll() default false;
}
